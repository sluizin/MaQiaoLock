package MaQiao.MaQiaoLock;

import java.lang.reflect.Field;
import java.util.LinkedList;
import sun.misc.Unsafe;
import MaQiao.Constants.Constants;
import static MaQiao.MaQiaoLock.Consts.booleanType;
import static MaQiao.MaQiaoLock.Consts.lockType;

/**
 * 第一期开发，只开发无锁与独占锁，后期会加上共享锁<br/>
 * <font color='red'> 注意：<br/>
 * 在多个含有MQlock的方法体嵌套调用时，会出现属性锁的重叠情况 容易出现死锁情况，请开发时注意！！！<br/>
 * 解决方法：<br/>
 * 线程组，用于方法嵌套时锁的重叠判断，已经解决，请使用initMasterState[3]打开，但最好关闭属性检查<br/>
 * 为防止线程对线程组的操作溢出，对线程组设置了独占锁，如果开启重叠判断,则每次操作必须获得这个独占锁<br/>
 * </font><br/>
 * @author Sunjian
 * @since 1.7
 * @version 1.1
 */
public final class MQlockMaster {
	private static final Unsafe UNSAFE = Constants.UNSAFE;
	/**
	 * 本Master的锁，进入者才能得到相关属性组的权限，但多线程时，只能接待一个人<br/>
	 */
	@SuppressWarnings("unused")
	private transient volatile int lockedMaster = booleanType.False.index;
	/**
	 * 系统的初始化状态，如是否开通主Master锁、是否开通锁属性的变化检查、是否开通在关闭锁时显示各属性情况<br/>
	 * 先用int[]数组保存状态值。不用booleanType[]，用于后期，在修改成UNSAFE.compareAndSwapInt对Int数组操作<br/>
	 * 如后期不修改成CAS更改状态，则移到Consts文件中当成static变量用<br/>
	 * 0:判断是否开启主Master锁<br/>
	 * 1:是否在关闭时进行锁属性是否发现变化的检查(最好在开发时开通，生产时关闭)<br/>
	 * 2:是否在关闭时显示各属性的变化情况<br/>
	 * 3:是否开通ThreadList 判断方法嵌套时锁的重叠<br/>
	 * 4:开通ThreadList 判断方法嵌套时锁的重叠，是否使用独占式<br/>
	 */
	static final transient int[] initMasterState = { booleanType.False.index,//0:判断是否开启主Master锁
			booleanType.True.index,//1:是否在关闭时进行锁属性是否发现变化的检查(最好在开发时开通，生产时关闭)
			booleanType.True.index,//2:是否在关闭时显示各属性的变化情况
			booleanType.True.index, //3:是否开通ThreadList 判断方法嵌套时锁的重叠
			booleanType.False.index //4:开通ThreadList 判断方法嵌套时锁的重叠，是否使用独占式
	};
	/**
	 * CAS线程组独占标识 状态:是否线程组占用
	 */
	@SuppressWarnings("unused")
	private transient volatile int threadLinkedMaster = booleanType.False.index;
	/**
	 * 线程组，用于方法嵌套时锁的重叠判断
	 */
	private transient volatile LinkedList<ThreadNode> ThreadLinked = new LinkedList<ThreadNode>();
	/**
	 * 含有锁的对象
	 */
	private transient Object obj = null;
	/**
	 * Group组，包含Group(唯一),状态:是否线程占用
	 */
	private transient GroupNode[] lockedGroups = null;
	/**
	 * 保存各属性 标志组
	 */
	private transient FieldNode[] lockedFields = null;

	@SuppressWarnings("unused")
	private MQlockMaster() {
	}

	/**
	 * 初始化<br/>
	 * obj:锁所在的对象<br/>
	 * @param obj Object
	 * @throws Error
	 */
	public MQlockMaster(final Object obj) throws Error {
		MQlockMasterInit(obj);
	}

	/**
	 * 初始化<br/>
	 * obj:锁所在的对象<br/>
	 * LockedGroups[0]:是否开通 所有主锁的独占模式<br/>
	 * LockedGroups[1]:是否开通 关闭时的锁属性的检查<br/>
	 * (<br/>
	 * 开通后，多线程只有允许一个线程检索各Group锁状态<br/>
	 * 关闭后，多线程争夺各组的锁，死锁状态可能性降底。争夺没有级别区分<br/>
	 * )<br/>
	 * @param obj Object
	 * @param LockedGroups booleanType[]
	 * @throws Error
	 */
	public MQlockMaster(final Object obj, final booleanType... LockedGroups) throws Error {
		for (int i = 0, len = LockedGroups.length, vlen = initMasterState.length; i < len; i++) {
			if (i > vlen) break;
			initMasterState[i] = LockedGroups[i].index;
		}
		MQlockMasterInit(obj);
	}

	private void MQlockMasterInit(final Object obj) throws Error {
		if (obj == null) throw new Error(Consts.ErrorNolock);
		final Field[] fields = obj.getClass().getDeclaredFields();
		int count = 0;
		for (Field field : fields)
			//获取字段中包含lock的注解  
			if (field.getAnnotation(lock.class) != null) count++;
		/*未发现锁注解*/
		if (count == 0) throw new Error(Consts.ErrorNolock);
		this.lockedFields = new FieldNode[count];
		lock lockThis = null;
		count = 0;
		for (Field field : fields) {
			/*获取字段中包含lock的注解*/
			lockThis = field.getAnnotation(lock.class);
			if (lockThis != null) {
				addArray(field);
				final FieldNode e = new FieldNode(field, lockThis.Group());
				if (initMasterState[1] == booleanType.True.index) e.identity = Consts.getIdentity(field, obj);
				this.lockedFields[count++] = e;
			}
		}
		this.obj = obj;
	}

	final boolean tryLock(final MQlock mqlock) throws Error {
		/*输入的锁组出现问题*/
		if (!Consts.ArrayContains(this.lockedGroups, mqlock.groups)) throw new Error(Consts.ErrorNolock);
		thisLockCas(lockType.None, lockType.Monopoly);
		try {
			if (initMasterState[3] == booleanType.True.index) {
				/*得到线程组锁*/
				if (initMasterState[4] == booleanType.True.index) while (!UNSAFE.compareAndSwapInt(this, Consts.threadLinkedMasterOffset, booleanType.False.index, booleanType.True.index)) {
				}
				int Index;
				if ((Index = this.ThreadLinked.indexOf(new ThreadNode(mqlock.thread))) != -1) {
					/*发现有此线程，则进入判断重叠锁组*/
					final int[] differenceSet = Consts.differenceSet(this.ThreadLinked.get(Index).GroupLinked, mqlock.groups);
					this.ThreadLinked.get(Index).GroupLinked.add(mqlock.groups);
					if (differenceSet.length > 0) {
						privateChangeStatesGroupsLock(differenceSet);
						this.ThreadLinked.get(Index).CASLinked.add(differenceSet);
						mqlock.groups = differenceSet;
					} else {
						this.ThreadLinked.get(Index).CASLinked.add(Consts.ArrayIntNull);
						mqlock.groups = Consts.ArrayIntNull;
					}

				} else {
					/*没有线程*/
					final int[] tryGroups = Consts.indexArrayGroup(this.lockedGroups, mqlock.groups);
					privateChangeStatesGroupsLock(tryGroups);
					this.ThreadLinked.add(new ThreadNode(mqlock.thread, mqlock.groups, tryGroups));
					mqlock.groups = tryGroups;
				}
				/*释放线程组锁*/
				if (initMasterState[4] == booleanType.True.index) UNSAFE.compareAndSwapInt(this, Consts.threadLinkedMasterOffset, booleanType.True.index, booleanType.False.index);
			} else {
				final int[] tryGroups = Consts.indexArrayGroup(this.lockedGroups, mqlock.groups);
				privateChangeStatesGroupsLock(tryGroups);
				mqlock.groups = tryGroups;
			}
			return true;
		} finally {
			thisLockCas(lockType.Monopoly, lockType.None);
		}
	}

	final boolean tryUnLock(final MQlock mqlock) throws Error {
		thisLockCas(lockType.None, lockType.Monopoly);
		try {
			if (initMasterState[3] == booleanType.True.index) {
				int Index;
				if ((Index = this.ThreadLinked.indexOf(new ThreadNode(mqlock.thread))) != -1) {
					/*发现有此线程，则进入判断重叠锁组*/
					/*
					 * 得到 threadLinkedMaster 锁，才能对线程组进行修改
					 */
					if (initMasterState[4] == booleanType.True.index) while (!UNSAFE.compareAndSwapInt(this, Consts.threadLinkedMasterOffset, booleanType.False.index, booleanType.True.index)) {
					}
					final ThreadNode f = this.ThreadLinked.get(Index);
					final int[] CASGroups = f.CASLinked.getLast();
					if (CASGroups.length > 0) {
						privateChangeStatesGroupsUnlock(CASGroups);
					}
					if (initMasterState[1] == booleanType.True.index) {
						final int[] Suffix = Consts.commonArraySuffixs(this.lockedGroups, mqlock.groups);
						updateVariable(Suffix);
						TestingVariable(f);
					}
					f.CASLinked.removeLast();
					//f.GroupLinked.removeLast();
					/*线程组清空，后期注意同步性*/
					for (int i = 0, size = this.ThreadLinked.size(); i < size; i++)
						if (this.ThreadLinked.get(i).CASLinked.isEmpty()) this.ThreadLinked.remove(i);
					if (initMasterState[4] == booleanType.True.index) UNSAFE.compareAndSwapInt(this, Consts.threadLinkedMasterOffset, booleanType.True.index, booleanType.False.index);
				} else {
					/*没有线程*/
					throw new Error(Consts.ErrorLockThread);
				}
				if (initMasterState[2] == booleanType.True.index) Consts.viewVariableChange(this.obj, lockedGroups, lockedFields, ThreadLinked);
			} else {
				final int[] Suffix = Consts.indexArraySuffixs(this.lockedGroups, mqlock.groups);
				privateChangeStatesSuffixUnlock(Suffix);
				if (initMasterState[2] == booleanType.True.index) Consts.viewVariableChange(this.obj, lockedGroups, lockedFields, ThreadLinked);
				if (initMasterState[1] == booleanType.True.index) {
					updateVariable(Suffix);
					TestingVariable(Suffix);
				}
			}
			return true;
		} finally {
			thisLockCas(lockType.Monopoly, lockType.None);
		}
	}

	/**
	 * 对多组进行锁定，流程：依次锁，发现冲突则依次释放已经锁定的锁，再进行依次锁(要锁定的目标锁组已经过滤、唯一)
	 * @param groups int[]
	 * @throws Error
	 */
	private final void privateChangeStatesGroupsLock(final int[] groups) throws Error {
		privateChangeStatesSuffixLock(Consts.commonArraySuffixs(this.lockedGroups, groups));
	}

	/**
	 * 对多组进行锁定，流程：依次锁，发现冲突则依次释放已经锁定的锁，再进行依次锁(要锁定的目标锁组已经过滤、唯一)
	 * @param Suffix int[]
	 * @throws Error
	 */
	private final void privateChangeStatesSuffixLock(final int[] Suffix) throws Error {
		int len;
		if ((len = Suffix.length) == 0) throw new Error(Consts.ErrorNolock);
		int i = -1, ii;
		try {
			loop: while (true) {//多线程要得到锁组 相互抢，但不会出现死锁状态
				for (i = 0; i < len; i++) {
					if (!tryLockStatesSuffix(Suffix[i], lockType.None, lockType.Monopoly)) {
						if (i > 0) for (ii = i - 1; ii >= 0; ii--)
							tryLockStatesSuffix(Suffix[ii], lockType.Monopoly, lockType.None);
						break;
					}
					if (i == (len - 1)) break loop;//如果是最后一个已经检查完成，则跳出死循环
				}
			}
		} finally {
			/*
			 * 如果在得锁组时出现异常，则释放所有锁
			 */
			if (i >= 0 && i < (len - 1)) {
				privateChangeStatesSuffixUnlock(Suffix);
				throw new Error(Consts.Errortrylock);
			}
		}
	}

	/**
	 * 放弃所有锁组，设置为0，无论成功与失败，都保证所有下标组中所有状态锁都为0
	 * @param groups int[]
	 * @throws Error
	 */
	private final void privateChangeStatesGroupsUnlock(final int[] groups) throws Error {
		privateChangeStatesSuffixUnlock(Consts.commonArraySuffixs(this.lockedGroups, groups));
	}

	/**
	 * 放弃所有锁组，设置为0，无论成功与失败，都保证所有下标组中所有状态锁都为0
	 * @param Suffix int[]
	 * @throws Error
	 */
	private final void privateChangeStatesSuffixUnlock(final int[] Suffix) throws Error {
		int len;
		if ((len = Suffix.length) == 0) throw new Error(Consts.ErrorNolock);
		for (int i = 0; i < len; i++)
			tryLockStatesSuffix(Suffix[i], lockType.Monopoly, lockType.None);
	}

	/**
	 * 尝试清除线程队列
	 */
	@SuppressWarnings("unused")
	private final void tryThreadLinkedClean() {
		if (UNSAFE.compareAndSwapInt(this, Consts.threadLinkedMasterOffset, booleanType.False.index, booleanType.True.index)) {
			for (int i = 0, size = this.ThreadLinked.size(); i < size; i++)
				if (this.ThreadLinked.get(i).GroupLinked.isEmpty()) this.ThreadLinked.remove(i);
			UNSAFE.compareAndSwapInt(this, Consts.threadLinkedMasterOffset, booleanType.True.index, booleanType.False.index);
		}
	}

	/**
	 * 更新下标的属性信息，用于存底，即：多个锁组信息更新
	 * @param Suffix
	 * @throws Error
	 */
	private final void updateVariable(final int[] Suffix) throws Error {
		int vlen;
		if ((vlen = Suffix.length) == 0) throw new Error(Consts.ErrorNolock);
		int len;
		if ((len = this.lockedFields.length) == 0) throw new Error(Consts.ErrorNolock);
		double newIdentity;
		for (int i = 0, ii; i < len; i++)
			for (ii = 0; ii < vlen; ii++)
				if (this.lockedFields[i].value == this.lockedGroups[Suffix[ii]].group && (newIdentity = Consts.getIdentity(this.lockedFields[i].field, this.obj)) != this.lockedFields[i].identity) this.lockedFields[i].identity = newIdentity;
	}

	/**
	 * 测试各属性是否发生变化(最好在开发时开通，生产时关闭)
	 * @throws Error
	 */
	private final void TestingVariable(final int[] Suffix) throws Error {
		int vlen;
		if ((vlen = Suffix.length) == 0) throw new Error(Consts.ErrorNolock);
		int len;
		if ((len = this.lockedFields.length) == 0) throw new Error(Consts.ErrorNolock);
		double newIdentity;
		for (int i = 0, ii; i < len; i++)
			for (ii = 0; ii < vlen; ii++)
				if (this.lockedFields[i].value != this.lockedGroups[Suffix[ii]].group && (newIdentity = Consts.getIdentity(this.lockedFields[i].field, this.obj)) != this.lockedFields[i].identity) {
					String str = "change[Group:" + this.lockedFields[i].value + "\tField:" + this.lockedFields[i].field.getName() + "\t]:(" + this.lockedFields[i].identity + ")->(" + newIdentity
							+ ")";
					System.out.println(str);
					throw new Error(Consts.ErrorAttribChange);
				}
	}

	/**
	 * 测试各属性是否发生变化(最好在开发时开通，生产时关闭)
	 * @throws Error
	 */
	private final void TestingVariable(final ThreadNode threadNode) throws Error {
		int vlen;
		if ((vlen = threadNode.GroupLinked.size()) == 0) throw new Error(Consts.ErrorNolock);
		int len;
		if ((len = this.lockedFields.length) == 0) throw new Error(Consts.ErrorNolock);
		double newIdentity;
		for (int i = 0, ii, iii, glen, find = booleanType.False.index; i < len; i++, find = booleanType.False.index) {
			loop: for (ii = 0; ii < vlen; ii++) {
				final int[] Groups = threadNode.GroupLinked.get(ii);
				for (iii = 0, glen = Groups.length; iii < glen; iii++) {
					if (this.lockedFields[i].value == Groups[iii]) {
						find = booleanType.True.index;
						break loop;
					}
				}
			}
			if (find == booleanType.False.index && (newIdentity = Consts.getIdentity(this.lockedFields[i].field, this.obj)) != this.lockedFields[i].identity) {
				//在线程组中未发现锁定项
				String str = "change[Group:" + this.lockedFields[i].value + "\tField:" + this.lockedFields[i].field.getName() + "\t]:(" + this.lockedFields[i].identity + ")->(" + newIdentity + ")";
				System.out.println(str);
				throw new Error(Consts.ErrorAttribChange);
			}
		}

	}

	/**
	 * 更改下标位置的状态值
	 * @param Index int
	 * @param from lockType
	 * @param to lockType
	 * @return boolean
	 */
	private final boolean tryLockStatesSuffix(final int Index, final lockType from, final lockType to) {
		return tryLockCas(this.lockedGroups[Index], Consts.GroupNodeStateOffset, from, to);
	}

	/**
	 * Cas本对象锁
	 * @param statesOffset long
	 * @param from lockType
	 * @param to lockType
	 * @return boolean
	 */
	private final boolean tryLockCas(final Object obj, final long statesOffset, final lockType from, final lockType to) {
		return UNSAFE.compareAndSwapInt(obj, statesOffset, from.index, to.index);
	}

	/**
	 * 添加锁类，如发现重复则不添加
	 * @param field Field
	 */
	private final void addArray(final Field field) {
		final lock lockThis = field.getAnnotation(lock.class);
		if (lockThis != null) {
			int len = 0;
			if (this.lockedGroups != null && (len = this.lockedGroups.length) > 0) for (int i = 0; i < len; i++)
				if (this.lockedGroups[i].group == lockThis.Group()) return;
			final GroupNode[] lockedGroups = new GroupNode[len + 1];
			if (len > 0) {
				System.arraycopy(this.lockedGroups, 0, lockedGroups, 0, len);
			}
			lockedGroups[len] = new GroupNode(lockThis.Group(), Consts.lockType.None.index);
			this.lockedGroups = lockedGroups;
		}
	}

	/**
	 * 进入本Master中必须要有权限，并且是唯一进入并得到内部权限，<br/>
	 * Cas本对象锁 使用死循环，排队进行队列进入<br/>
	 * @param from lockType
	 * @param to lockType
	 */
	private final void thisLockCas(final lockType from, final lockType to) {
		if (initMasterState[0] == booleanType.True.index) while (!UNSAFE.compareAndSwapInt(this, Consts.lockedMasterOffset, from.index, to.index)) {
			/*
			 * 等待中....
			 */
			System.out.println(Consts.BusyAlert);
			/*
			 * 尝试暂时停止线程，等一下一段时间
			 */
			//UNSAFE.park(false, 0L);
		}
	}

	final class GroupNode {
		/**
		 * Groups:用于保存Group标志组 唯一<br/>
		 */
		int group;
		/**
		 * States:状态 0:未占用 2:已独占锁定(与this.Groups相对应)<br/>
		 */
		int state;

		private GroupNode() {

		}

		private GroupNode(final int group, final int state) {
			this.group = group;
			this.state = state;
		}

		@Override
		public String toString() {
			return "GroupNode [group=" + group + ", state=" + state + "]";
		}

	}

	final class FieldNode {
		Field field = null;
		int value = 0;
		double identity = 0;

		private FieldNode() {
		}

		private FieldNode(final Field field, final int value) {
			this.field = field;
			this.value = value;
		}

		private FieldNode(final Field field, final int value, final double identity) {
			this.field = field;
			this.value = value;
			this.identity = identity;
		}

		@Override
		public String toString() {
			return "FieldNode [field=" + field.getName() + ", value=" + value + ", identity=" + identity + "]";
		}

	}

	final class ThreadNode {
		Thread thread;
		LinkedList<int[]> GroupLinked = new LinkedList<int[]>();
		LinkedList<int[]> CASLinked = new LinkedList<int[]>();

		private ThreadNode() {

		}

		private ThreadNode(final Thread thread) {
			this.thread = thread;
		}

		private ThreadNode(final Thread thread, final int[] groups, final int[] CASInt) {
			this.thread = thread;
			this.GroupLinked.add(groups);
			this.CASLinked.add(CASInt);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			//result = prime * result + ((GroupLinked == null) ? 0 : GroupLinked.hashCode());
			result = prime * result + ((thread == null) ? 0 : thread.hashCode());
			return result;
		}

		/**
		 * 在LinkedList调用indexOf时，需要调用equals()，但ThreadNode只判断thread是否相同，不判断第二属性Groups
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			ThreadNode other = (ThreadNode) obj;
			if (!getOuterType().equals(other.getOuterType())) return false;
			if (thread == null) {
				if (other.thread != null) return false;
			} else if (!thread.equals(other.thread)) return false;
			return true;
		}

		private MQlockMaster getOuterType() {
			return MQlockMaster.this;
		}

	}

}
