package com.example.bluromatic.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.bluromatic.DELAY_TIME_MILLIS
import com.example.bluromatic.KEY_BLUR_LEVEL
import com.example.bluromatic.KEY_IMAGE_URI
import com.example.bluromatic.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val TAG = "BlurWorker"

//CoroutineWorker를 상속받은 BlurWorker
class BlurWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    //수행해야 하는 작업
    //Kotlin 코루틴과 상호 운용되면서 비동기로 실행됨
    override suspend fun doWork(): Result {
        //받은 입력데이터로부터 리소스 URI와 블러 수준 가져오기
        val resourceUri = inputData.getString(KEY_IMAGE_URI)
        val blurLevel = inputData.getInt(KEY_BLUR_LEVEL, 1)

        //상태 알림 표시 & 이미지를 블러 처리한다고 사용자에게 알림
        makeStatusNotification(
            applicationContext.resources.getString(R.string.blurring_image),
            applicationContext
        )

        //실제 이미지 블러 작업이 실행되는 부분
        //디스패처를 명시하지 않으면 기본적으로 Dispatchers.Default로 실행된다.
        //일반적인 코루틴에서 하는것처럼 withContext()를 사용하여 디스패처를 변경해줄 수 있다.
        return withContext(Dispatchers.IO) {
            return@withContext try {
                //require() 문 사용
                //조건문이 false이면 블록안의 메시지가 전달되면서 IllegalArgumentException이 발생한다.
                require(!resourceUri.isNullOrBlank()) {
                    val errorMessage =
                        applicationContext.resources.getString(R.string.invalid_input_uri)
                    Log.e(TAG, errorMessage)
                    errorMessage
                }

                //리소스 ID로 비트맵 생성
                /*val picture = BitmapFactory.decodeResource(
                    applicationContext.resources,
                    R.drawable.android_cupcake
                )*/

                //URI를 통해서 비트맵 생성
                //콘텐츠 리졸버 생성, URI가 가리키는 콘텐츠를 받기 위해서 필요하다
                val resolver = applicationContext.contentResolver

                delay(DELAY_TIME_MILLIS)

                //블러 처리할 이미지 비트맵
                val picture = BitmapFactory.decodeStream(
                    resolver.openInputStream(Uri.parse(resourceUri))
                )

                //블러 처리한 이미지를 output이라는 변수에 저장
                val output = blurBitmap(picture, blurLevel)

                //writeBitmapToFile() -> 비트맵을 임시 파일에 쓴 뒤 해당 파일의 URI를 반환
                val outputUri = writeBitmapToFile(applicationContext, output)

                //출력 데이터 생성
                val outputData = workDataOf(KEY_IMAGE_URI to outputUri.toString())

                Result.success(outputData) //작업이 성공적으로 처리됐음을 의미하는 Result 객체, success()에 데이터를 인수로 넣으면 해당 데이터가 출력 데이터가 된다.
            } catch(throwable: Throwable) {
                //이미지 블러 처리 중 오류 발생 시 오류 로그를 표시
                Log.e(
                    TAG,
                    applicationContext.resources.getString(R.string.error_applying_blur),
                    throwable
                )

                Result.failure() //작업을 실패했음을 의미하는 Result 객체
            }
        }
    }
}