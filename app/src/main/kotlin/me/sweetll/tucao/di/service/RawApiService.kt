package me.sweetll.tucao.di.service

import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface RawApiService {
    @GET(ApiConfig.DANMU_API_URL)
    fun danmu(@Query("playerID") playerId: String,
              @Query("r") r: Long) : Observable<ResponseBody>

    @GET(ApiConfig.INDEX_URL)
    @Headers("Cookie: tucao_verify=ok")
    fun index() : Observable<ResponseBody>

    @GET(ApiConfig.LIST_URL)
    @Headers("Cookie: tucao_verify=ok")
    fun list(@Path("tid") tid: Int): Observable<ResponseBody>

    @GET(ApiConfig.BGM_URL)
    @Headers("Cookie: tucao_verify=ok")
    fun bgm(@Path("year") year: Int,
            @Path("month") month: Int): Observable<ResponseBody>

    @FormUrlEncoded
    @POST(ApiConfig.SEND_DANMU_URL)
    @Headers("Cookie: tucao_verify=ok")
    fun sendDanmu(@Query("playerID") playerId: String,
                  @Field("cid") cid: String,
                  @Field("stime") stime: Float,
                  @Field("message") message: String,
                  @Field("user") user: String = "test",
                  @Field("size") size: Long = 25,
                  @Field("mode") mode: Int = 1,
                  @Field("color") color: Int = 16777215): Observable<ResponseBody>

    @GET(ApiConfig.COMMENT_URL)
    @Headers("Cookie: tucao_verify=ok")
    fun comment(@Query("commentid") commentId: String,
                @Query("page") page: Int): Observable<ResponseBody>

    @FormUrlEncoded
    @POST(ApiConfig.SEND_COMMENT_URL)
    fun sendComment(@Query("commentid") commentId: String,
                    @Field("content") content: String): Observable<ResponseBody>

    @GET
    @Streaming
    fun download(@Url url: String): Observable<Response<ResponseBody>>

    @GET(ApiConfig.CODE_URL)
    fun checkCode(): Observable<ResponseBody>

    @GET(ApiConfig.LOGIN_URL)
    fun login_get(): Observable<ResponseBody>

    @FormUrlEncoded
    @POST(ApiConfig.LOGIN_URL)
    fun login_post(@Field("username") username: String,
                   @Field("password") password: String,
                   @Field("code") code: String,
                   @Field("cookietime") cookietime: Int = 31536000,
                   @Field("dosubmit") dosubmit: String = "登录"): Observable<ResponseBody>

    @GET(ApiConfig.LOGOUT_URL)
    fun logout(): Observable<ResponseBody>


    @GET(ApiConfig.PERSONAL_URL)
    fun personal(): Observable<ResponseBody>

    @GET(ApiConfig.USER_URL)
    fun user(@Path("userid") userid: String): Observable<ResponseBody>

    @GET(ApiConfig.SPACE_URL)
    fun space(@Query("uid") uid: String,
              @Query("page") page: Int): Observable<ResponseBody>

}
