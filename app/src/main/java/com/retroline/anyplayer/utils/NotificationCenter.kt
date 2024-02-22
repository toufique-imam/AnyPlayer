package com.retroline.anyplayer.utils

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject

class NotificationCenter {
    private val subject: PublishSubject<Any> = PublishSubject.create()

    fun post(event: Any){
        subject.onNext(event)
    }
    fun observe(): Observable<Any> {
        return subject
    }

    companion object {
        val shared = NotificationCenter()
        const val PermissionCallback = "PermissionCallback"
    }
}