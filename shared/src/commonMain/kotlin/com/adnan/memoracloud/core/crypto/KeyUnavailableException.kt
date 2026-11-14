package com.adnan.memoracloud.core.crypto

class KeyUnavailableException(name: String) :
    IllegalStateException("Key '$name' cannot be unwrapped on this device")
