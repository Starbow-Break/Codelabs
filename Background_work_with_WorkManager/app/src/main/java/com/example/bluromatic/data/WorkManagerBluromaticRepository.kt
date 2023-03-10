/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bluromatic.data

import android.content.Context
import android.net.Uri
import androidx.work.*
import com.example.bluromatic.KEY_BLUR_LEVEL
import com.example.bluromatic.KEY_IMAGE_URI
import com.example.bluromatic.getImageUri
import com.example.bluromatic.workers.BlurWorker
import com.example.bluromatic.workers.CleanupWorker
import com.example.bluromatic.workers.SaveImageToFileWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class WorkManagerBluromaticRepository(context: Context) : BluromaticRepository {

    private val imageUri: Uri = context.getImageUri() //이미지 URI
    private val workManager = WorkManager.getInstance(context) //WorkManager 인스턴스 생성

    override val outputWorkInfo: Flow<WorkInfo?> = MutableStateFlow(null)

    /**
     * Create the WorkRequests to apply the blur and save the resulting image
     * @param blurLevel The amount to blur the image
     */
    override fun applyBlur(blurLevel: Int) {
        //WorkRequest 체인을 생성 (정확히는 WorkContinuation 객체를 생성)
        //처음 실행되는 WorkRequest는 CleanupWorker
        //OneTimeWorkRequest.from(Class)을 호출해서 OneTimeWorkRequest 객체를 생성할 수 있다.
        var continuation = workManager.beginWith(OneTimeWorkRequest.from(CleanupWorker::class.java))

        val blurBuilder = OneTimeWorkRequestBuilder<BlurWorker>() //BlurWorker에 대한 OneTimeWorkRequest 객체를 생성하는 빌더 생성
        blurBuilder.setInputData(createInputDataForWorkRequest(blurLevel, imageUri)) //blurBuilder에 입력 데이터를 전달
        continuation = continuation.then(blurBuilder.build()) //CleanupWorker 다음으로 BlurWorker가 실행

        val save = OneTimeWorkRequestBuilder<SaveImageToFileWorker>().build()
        continuation = continuation.then(save) //BlurWorker 다음으로 SaveImageToFileWorker가 실행

        continuation.enqueue() //WorkRequest 체인 실행
    }

    /**
     * Cancel any ongoing WorkRequests
     * */
    override fun cancelWork() {}

    /**
     * Creates the input data bundle which includes the blur level to
     * update the amount of blur to be applied and the Uri to operate on
     * @return Data which contains the Image Uri as a String and blur level as an Integer
     */
    private fun createInputDataForWorkRequest(blurLevel: Int, imageUri: Uri): Data {
        val builder = Data.Builder()
        builder.putString(KEY_IMAGE_URI, imageUri.toString()).putInt(KEY_BLUR_LEVEL, blurLevel)
        return builder.build()
    }
}
