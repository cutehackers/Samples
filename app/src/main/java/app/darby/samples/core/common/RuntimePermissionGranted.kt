package app.darby.samples.core.common

/**
 * @param value request code
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
annotation class RuntimePermissionGranted(val value: Int)
