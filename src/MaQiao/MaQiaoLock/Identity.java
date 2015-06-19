package MaQiao.MaQiaoLock;

import java.lang.reflect.Field;

public final class Identity {
	public static final double get(final Object obj,final Field field){
		return get(Consts.reflectionFactory.newFieldAccessor(field, false).get(obj));
	}
	public static final double get(final Object obj){
		if(obj == null)return 0;
		if(obj instanceof Integer)return toDouble((Integer)obj);
		if(obj instanceof Boolean)return toDouble((Boolean)obj);
		if(obj instanceof Float)return toDouble((Float)obj);
		if(obj instanceof Long)return toDouble((Long)obj);
		if(obj instanceof Double)return toDouble((double)obj);
		if(obj instanceof Byte)return toDouble((Byte)obj);
		if(obj instanceof Short)return toDouble((Short)obj);
		if(obj instanceof String)return toDouble((String)obj);
		return toDouble(obj);
	}
	static final double toDouble(final int i){
		return (double)i;
	}
	static final double toDouble(final Integer i){
		return (double)i;
	}
	static final double toDouble(final char i){
		return toDouble(Integer.parseInt(i+""));
	}
	static final double toDouble(final boolean i){
		return toDouble(Integer.parseInt(i+""));
	}
	static final double toDouble(final Boolean i){
		return toDouble(Integer.parseInt(i+""));
	}
	static final double toDouble(final float i){
		return (double)i;
	}
	static final double toDouble(final Float i){
		return (double)i;
	}
	static final double toDouble(final long i){
		return (double)i;
	}
	static final double toDouble(final Long i){
		return (double)i;
	}
	static final double toDouble(final double i){
		return i;
	}
	static final double toDouble(final Double i){
		return i;
	}
	static final double toDouble(final byte i){
		return (double)i;
	}
	static final double toDouble(final Byte i){
		return (double)i;
	}
	static final double toDouble(final short i){
		return (double)i;
	}
	static final double toDouble(final Short i){
		return (double)i;
	}
	static final double toDouble(final String i){
		return (double)i.hashCode();
	}
	static final double toDouble(final Object obj){
		return (double)obj.hashCode();
	}
}
