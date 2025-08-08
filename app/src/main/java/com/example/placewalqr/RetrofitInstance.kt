import com.example.placewalqr.ApiService
import com.example.placewalqr.LoggingInterceptor

object RetrofitInstance {
    private val client = okhttp3.OkHttpClient.Builder()
        .addInterceptor(LoggingInterceptor()) // 👈 aggiunto qui
        .build()

    val apiService: ApiService by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl("https://placewalqr.pythonanywhere.com/") // il tuo backend
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ApiService::class.java)
    }
}
