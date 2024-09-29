package com.jetbrains.micropython.nova

open class MpyCommForTest(errorLogger: (Throwable) -> Any = {}): MpyComm(errorLogger) {
    public override fun isTtySuspended(): Boolean  = super.isTtySuspended()
}