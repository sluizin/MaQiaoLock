package MaQiao.MaQiaoLock;
//import java.lang.annotation.ElementType;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * 对象组锁<br/>
 * 参数：Group=1/2/3...<br/>
 * 获得多个属性的同步<br/>
 * @author Sunjian
 * @since 1.7
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({FIELD})
@Documented
public @interface lock {
	int Group() default 0;

}
