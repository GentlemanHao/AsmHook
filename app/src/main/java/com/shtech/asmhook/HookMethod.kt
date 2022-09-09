package com.shtech.asmhook

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@kotlin.annotation.Retention(AnnotationRetention.BINARY)
annotation class HookMethod(val ownerClass: KClass<*>, val methodName: String = "")