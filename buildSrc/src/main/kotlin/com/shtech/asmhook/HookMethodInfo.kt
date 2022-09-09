package com.shtech.asmhook

class HookMethodInfo(val ownerClass: String, val methodName: String, val methodDesc: String) {

    val className: String = ownerClass.substring(1 until ownerClass.length - 1)

}