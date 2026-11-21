package com.koch.weather.data.mapper

/**
 * «Хост не найден» (DNS-ошибка): на Android это java.net.UnknownHostException,
 * на iOS — NSError NSURLErrorDomain/-1003 внутри DarwinHttpRequestException.
 *
 * В Ktor 3 отдельного класса UnknownHostException нет (проверено по
 * ktor-client-core 3.6.1), поэтому общий код различает DNS-ошибку через этот
 * expect/actual — чтобы текст ошибки UI остался 1:1 с Android-версией
 * («Нет соединения с сервером погоды…» вместо общего «Ошибка сети…»).
 */
expect fun isUnknownHostFailure(error: Throwable): Boolean
