package net.sf.appstatus.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.sf.appstatus.StatusService;
import net.sf.appstatus.check.impl.StatusResult;

/**
 * Annotate a check method.
 * 
 * The annotated method should not have any parameter and must return a
 * {@link StatusResult} result, otherwise an error will occur
 * 
 * @see StatusService
 * @see StatusService#isACheckMethod()
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface AppCheckMethod {
}