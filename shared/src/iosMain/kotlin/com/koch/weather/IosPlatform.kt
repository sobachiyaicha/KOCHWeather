package com.koch.weather

import platform.UIKit.UIDevice

/**
 * iOS-специфичные данные. Swift видит это как `IosPlatform.shared`.
 */
object IosPlatform {
    val systemName: String = UIDevice.currentDevice.systemName()
    val systemVersion: String = UIDevice.currentDevice.systemVersion
    val model: String = UIDevice.currentDevice.model

    fun summary(): String = "$systemName $systemVersion · $model"
}
