package MaQiao.MaQiaoLock;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.util.LinkedList;
import sun.misc.Unsafe;
import sun.reflect.ReflectionFactory;
import MaQiao.Constants.Constants;
import MaQiao.MaQiaoLock.MQlockMaster.GroupNode;
import MaQiao.MaQiaoLock.MQlockMaster.FieldNode;
import MaQiao.MaQiaoLock.MQlockMaster.ThreadNode;

/**
 * 常量:<br/>
 * @author Sunjian
 */
public final class Consts {
	static final String ErrorNolock = "no find Lock!!";
	static final String BusyAlert = "Busy..Thread Hold..retry!!!!";
	static final String Errortrylock = "when tryLock Groups Error than unLock all Groups!!";
	static final String ErrorNotMaster = "no find MQlockMaster Attribute!!";
	static final String ErrorAttribChange = "locked_Attribute not locked but is changed!!!";/*"发现有锁状态下的属性发现未判断锁的更改!!"*/
	static final String ErrorLockThread = "when tryUnlock but ThreadList is Error(no find thread)!!";
	static final int[] ArrayIntNull = new int[0]; /* 空数组，用于返回空值 */
	/**
	 * int[]数组地址偏移量
	 */
	static final long ArrayAddress = Constants.UNSAFE.arrayBaseOffset(int[].class);
	/**
	 * Unsafe.ARRAY_INT_INDEX_SCALE
	 */
	static final int IntScale = Unsafe.ARRAY_INT_INDEX_SCALE;
	/**
	 * MQlockMaster 中的 lockedMaster(int)属性的偏移量
	 */
	static long lockedMasterOffset = 0L;
	/**
	 * MQlockMaster.GroupNode 中的 state(int)属性的偏移量
	 */
	static long GroupNodeStateOffset = 0L;
	/**
	 * MQlockMaster 中的 ThreadLinked线程要锁定的(int)属性的偏移量
	 */
	static long threadLinkedMasterOffset = 0L;

	static {
		try {
			lockedMasterOffset = Constants.UNSAFE.objectFieldOffset(MQlockMaster.class.getDeclaredField("lockedMaster"));
			GroupNodeStateOffset = Constants.UNSAFE.objectFieldOffset(GroupNode.class.getDeclaredField("state"));
			threadLinkedMasterOffset = Constants.UNSAFE.objectFieldOffset(MQlockMaster.class.getDeclaredField("threadLinkedMaster"));
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
	}
	/**
	 * Constants.reflectionFactory.newFieldAccessor(f, false).get(obj)
	 */
	static final ReflectionFactory reflectionFactory = AccessController.doPrivileged(new sun.reflect.ReflectionFactory.GetReflectionFactoryAction());

	/**
	 * 判断锁组是否出现不同
	 * @param lockedGroups MQlockMaster.GroupNode[]
	 * @param groups int[]
	 * @return boolean
	 */
	static final boolean ArrayContains(final GroupNode[] lockedGroups, final int... groups) {
		int len;
		if ((len = groups.length) == 0) return false;
		for (int i = 0; i < len; i++)
			if (ArrayIndexOf(lockedGroups, groups[i]) == -1) return false;
		return true;
	}

	/**
	 * 判断某个参数锁组在源锁组中的位置
	 * @param lockedGroups MQlockMaster.GroupNode[]
	 * @param Index int
	 * @return int
	 */
	static final int ArrayIndexOf(final MQlockMaster.GroupNode[] lockedGroups, final int Index) {
		int len;
		if ((len = lockedGroups.length) == 0) return -1;
		for (int i = 0; i < len; i++)
			if (Index == lockedGroups[i].group) return i;
		return -1;
	}

	/**
	 * 参数锁组在源锁组中的下标集合，过滤多余参数锁组与无效参数锁组
	 * @param lockedGroups MQlockMaster.GroupNode[]
	 * @param groups int[]
	 * @return int[]
	 */
	static final int[] indexArraySuffixs(final GroupNode[] lockedGroups, final int... groups) {
		int len;
		if (lockedGroups == null || (len = lockedGroups.length) == 0) return ArrayIntNull;
		int vlen;
		if (groups == null || (vlen = groups.length) == 0) return ArrayIntNull;
		int i, ii, iii, count = i = 0;
		for (; i < vlen; i++)
			loop: for (ii = 0; ii < len; ii++)
				if (groups[i] == lockedGroups[ii].group) {
					if (i > 0) for (iii = i - 1; iii >= 0; iii--)
						if (groups[i] == groups[iii]) break loop;
					count++;
					break;
				}
		if (count > 0) {
			final int[] Array = new int[count];
			int p = i = 0;
			for (; i < vlen; i++)
				loop: for (ii = 0; ii < len; ii++)
					if (groups[i] == lockedGroups[ii].group) {
						if (i > 0) for (iii = i - 1; iii >= 0; iii--)
							if (groups[i] == groups[iii]) break loop;
						Array[p++] = ii;
						break;
					}
			return Array;
		}
		return ArrayIntNull;
	}

	/**
	 * 参数锁组在源锁组中的集合，过滤多余参数锁组与无效参数锁组
	 * @param lockedGroups MQlockMaster.GroupNode[]
	 * @param groups int[]
	 * @return int[]
	 */
	static final int[] indexArrayGroup(final GroupNode[] lockedGroups, final int... groups) {
		int len;
		if (lockedGroups == null || (len = lockedGroups.length) == 0) return ArrayIntNull;
		int vlen;
		if (groups == null || (vlen = groups.length) == 0) return ArrayIntNull;
		int i, ii, iii, count = i = 0;
		for (; i < vlen; i++)
			loop: for (ii = 0; ii < len; ii++)
				if (groups[i] == lockedGroups[ii].group) {
					if (i > 0) for (iii = i - 1; iii >= 0; iii--)
						if (groups[i] == groups[iii]) break loop;
					count++;
					break;
				}
		if (count > 0) {
			final int[] Array = new int[count];
			for (int p = i = 0; i < vlen; i++)
				loop: for (ii = 0; ii < len; ii++)
					if (groups[i] == lockedGroups[ii].group) {
						if (i > 0) for (iii = i - 1; iii >= 0; iii--)
							if (groups[i] == groups[iii]) break loop;
						Array[p++] = lockedGroups[ii].group;
						break;
					}
			return Array;
		}
		return ArrayIntNull;
	}

	/**
	 * 源锁组中，下标组中的Group组
	 * @param lockedGroups GroupNode[]
	 * @param Suffix int[]
	 * @return int[]
	 */
	static final int[] commonArrayGroups(final GroupNode[] lockedGroups, final int[] Suffix) {
		if (lockedGroups == null || lockedGroups.length == 0) return ArrayIntNull;
		int vlen;
		if (Suffix == null || (vlen = Suffix.length) == 0) return ArrayIntNull;
		final int[] Array = new int[vlen];
		for (int i = 0; i < vlen; i++)
			Array[i] = lockedGroups[Suffix[i]].group;
		return Array;
	}

	/**
	 * 源锁组中，下标组中的Suffix组
	 * @param Groups int[]
	 * @param groups int[]
	 * @return int[]
	 */
	static final int[] commonArraySuffixs(final GroupNode[] lockedGroups, final int[] groups) {
		int len;
		if (lockedGroups == null || (len = lockedGroups.length) == 0) return ArrayIntNull;
		int vlen;
		if (groups == null || (vlen = groups.length) == 0) return ArrayIntNull;
		final int[] Array = new int[vlen];
		for (int i = 0, ii; i < vlen; i++) {
			for (ii = 0; ii < len; ii++)
				if (lockedGroups[ii].group == groups[i]) {
					Array[i] = ii;
					break;
				}
		}
		return Array;
	}

	/**
	 * 差集
	 * @param GroupLinked LinkedList< int[] >
	 * @param groups int[]
	 * @return int[]
	 */
	static final int[] differenceSet(final LinkedList<int[]> GroupLinked, final int[] groups) {
		int len;
		if (GroupLinked == null || (len = GroupLinked.size()) == 0) return ArrayIntNull;
		int vlen;
		if (groups == null || (vlen = groups.length) == 0) return ArrayIntNull;
		int i, ii, count = 0;
		for (i = 0; i < vlen; i++)
			for (ii = 0; ii < len; ii++)
				if (indexOf(GroupLinked.get(ii), groups[i]) > -1) {
					count++;
					break;
				}
		if ((count = vlen - count) == 0) return ArrayIntNull;
		final int[] newArray = new int[count];
		for (i = count = 0; i < vlen; i++)
			for (ii = 0; ii < len; ii++) {
				if (indexOf(GroupLinked.get(ii), groups[i]) > -1) break;
				if (ii == len - 1) newArray[count++] = groups[i];
			}
		return newArray;
	}

	static final int indexOf(final int[] groups, final int group) {
		for (int i = 0, len = groups.length; i < len; i++)
			if (groups[i] == group) return i;
		return -1;
	}

	/**
	 * 得到某个有锁的属性double值
	 * @param field Field
	 * @param obj Object
	 * @return Double
	 */
	static final Double getIdentity(final Field field, final Object obj) {
		return Constants.getIdentityHashCode(obj, field);
		//return System.identityHashCode(reflectionFactory.newFieldAccessor(field, false).get(obj));
	}

	private static final void showGroups(final GroupNode[] lockedGroups) {
		int len = lockedGroups.length;
		StringBuilder sb = new StringBuilder(200);
		sb.append("+-------+---------------+---------------+\n");
		sb.append("|Num\t|\tGroups\t|\tStates\t|\n");
		sb.append("+-------+---------------+---------------+\n");
		for (int i = 0; i < len; i++) {
			sb.append("|" + (i + 1) + "\t|\t" + lockedGroups[i].group + "\t|\t" + lockedGroups[i].state + "\t|\n");
			if (i < len - 1) sb.append("+-------+---------------+---------------+\n");
		}
		sb.append("+-------+---------------+---------------+\n");
		System.out.println(sb.toString());
	}

	/**
	 * 测试各属性是否发生变化
	 * @param obj Object
	 * @param lockedGroups GroupNode[]
	 * @param lockedFields Node[]
	 * @throws Error
	 */
	static final void viewVariableChange(final Object obj, final GroupNode[] lockedGroups, final FieldNode[] lockedFields, final LinkedList<ThreadNode> ThreadLinked) throws Error {
		int len;
		System.out.println("=====================================================================");
		System.out.println("状态");
		System.out.println("lockedGroups\t:" + lockedGroups.length);
		showGroups(lockedGroups);
		len = lockedFields.length;
		System.out.println("=========================");
		System.out.println("lockedFields\t:" + len);
		System.out.println("=========================");

		System.out.println("---------------------------------------------------------------");
		System.out.println("\t    Fields\t\t    Values\t    Identity");
		System.out.println("---------------------------------------------------------------");
		for (int i = 0; i < len; i++) {
			System.out.println((i + 1) + "\t    " + lockedFields[i].field.getName() + "\t\t    " + lockedFields[i].value + "\t\t    " + lockedFields[i].identity);
			System.out.println("---------------------------------------------------------------");
		}
		if (MQlockMaster.initMasterState[3] == booleanType.True.index) {
			System.out.println("---------------------------------------------------------------");
			System.out.println("Thread:" + ThreadLinked.size());
			System.out.println("------------------------+---------------------------------------------");
			for (int i = 0; i < ThreadLinked.size(); i++) {
				System.out.print("|" + ThreadLinked.get(i).thread + "\t|");
				for (int ii = 0; ii < ThreadLinked.get(i).GroupLinked.size(); ii++)
					System.out.print(viewIntGroups(ThreadLinked.get(i).GroupLinked.get(ii)) + "|");
				System.out.println();
			}
			System.out.println("------------------------+---------------------------------------------");
		}
		System.out.println("=========================");
		System.out.println("lockedFieldsChanged:");
		System.out.println("=========================");
		double newsIdentity;
		for (int i = 0; i < len; i++)
			if ((newsIdentity = Consts.getIdentity(lockedFields[i].field, obj)) != lockedFields[i].identity) System.out.println("change[Group:" + lockedFields[i].value + "\tField:"
					+ lockedFields[i].field.getName() + "\t]:(" + lockedFields[i].identity + ")->(" + newsIdentity + ")");
		System.out.println("=====================================================================");
	}

	static final String viewIntGroups(final int[] Groups) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0, len = Groups.length; i < len; i++) {
			sb.append(Groups[i]);
			if (i != len - 1) sb.append(',');
		}
		return sb.toString();
	}

	/**
	 * 锁的状态:<br/>
	 * None(0):无锁<br/>
	 * Share(1):共享锁<br/>
	 * Monopoly(2):独占锁<br/>
	 * @author Sunjian
	 */
	static enum lockType {
		/**
		 * 无锁(0)
		 */
		None(0),
		/**
		 * 共享锁(1)
		 */
		Share(1),
		/**
		 * 独占锁(2)
		 */
		Monopoly(2);
		/**
		 * 0:无锁<br/>
		 * 1:共享锁<br/>
		 * 2:独占锁<br/>
		 */
		int index;

		// 构造方法
		private lockType(final int index) {
			this.index = index;
		}

		public static lockType getLockType(final int index) {
			for (lockType c : lockType.values())
				if (c.index == index) return c;
			return null;
		}
	}

	/**
	 * boolean的状态:<br/>
	 * False(0):假<br/>
	 * True(1):真<br/>
	 * @author Sunjian
	 */
	public static enum booleanType {
		/**
		 * 假(0)
		 */
		False(0),
		/**
		 * 真(1)
		 */
		True(1);
		/**
		 * False:假(0)<br/>
		 * True:真(1)<br/>
		 */
		int index;

		// 构造方法
		private booleanType(final int index) {
			this.index = index;
		}

		public static final booleanType getBooleanType(final int index) {
			for (booleanType c : booleanType.values())
				if (c.index == index) return c;
			return null;
		}
	}
}
