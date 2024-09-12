import com.google.net.cronet.okhttptransport.CronetInterceptor
import com.redelf.commons.net.cronet.Cronet
import okhttp3.OkHttpClient

fun OkHttpClient.Builder.useCronet(): OkHttpClient.Builder {

    Cronet.obtain()?.let {

        val cronetInterceptor = CronetInterceptor.newBuilder(it).build()

        return addInterceptor(cronetInterceptor)
    }

    return this
}