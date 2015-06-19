package MaQiao.MaQiaoLock;

import java.lang.reflect.Field;

/**
 * try(MQlock mqlock=new MQlock(lockMaster,5,1,4,7,8)){<br/>
 * try(MQlock mqlock=new MQlock(lockMaster)){<br/>
 * try(MQlock mqlock=new MQlock(this,8,5,12,22)){<br/>
 * try(MQlock mqlock=new MQlock(this)){<br/>
 * @author Sunjian
 * @since 1.7
 * @version 1.1
 */
public final class MQlock implements AutoCloseable {
	private transient MQlockMaster lockMaster = null;
	transient int[] groups;
	final transient Thread thread=Thread.currentThread();

	@SuppressWarnings("unused")
	private MQlock() {
	}

	public MQlock(final MQlockMaster lockMaster, final int... groups) throws Error {
		initMQlock(lockMaster, groups);
	}

	public MQlock(final MQlockMaster lockMaster)  throws Error{
		initMQlock(lockMaster, 0);
	}

	private final void initMQlock(final MQlockMaster lockMaster, final int... groups) throws Error {
		if (lockMaster == null) throw new Error(Consts.ErrorNotMaster);
		this.lockMaster = lockMaster;
		this.groups = groups;
		tryLock();
	}

	public MQlock(final Object obj, final int... groups)  throws Error{
		initMQlock(obj, groups);
	}

	public MQlock(final Object obj) throws Error {
		initMQlock(obj, 0);
	}

	private final void initMQlock(final Object obj, final int... groups) throws Error {
		if (obj == null) throw new Error(Consts.ErrorNotMaster);
		final Field[] fields = obj.getClass().getDeclaredFields();
		for (Field f : fields) {
			if (f.getType() == MQlockMaster.class) {
				//f.setAccessible(true);
				this.lockMaster = (MQlockMaster) (Consts.reflectionFactory.newFieldAccessor(f, false).get(obj));
				break;
			}
		}
		if (lockMaster == null) throw new Error(Consts.ErrorNotMaster);
		this.groups = groups;
		tryLock();
	}

	/**
	 * 锁定组
	 */
	private final void tryLock() throws Error {
		this.lockMaster.tryLock(this);
	}

	/**
	 * 释放组
	 */
	@Override
	public void close() throws Error {
		this.lockMaster.tryUnLock(this);
	}
}
