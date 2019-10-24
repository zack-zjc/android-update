
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.support.v4.content.FileProvider
import java.io.File


/**
 * @Author zack
 * @Date 2019/10/24
 * @Description 通用方法
 * @Version 1.0
 */

object CommonUtil {

    /**
     * 安装apk
     */
    fun installApk(context: Context,apkFilePath: String,providerPath: String ="${context.packageName}.FileProvider"){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            //适配Android Q,注意apkFilePath是通过ContentResolver得到的
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(apkFilePath), "application/vnd.android.package-archive")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)
        }else{
            val apkFile = File(apkFilePath)
            if (!apkFile.exists()) return
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val contentUri = FileProvider.getUriForFile(context, providerPath, apkFile)
                intent.setDataAndType(contentUri, "application/vnd.android.package-archive")
            } else {
                intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
            }
            context.startActivity(intent)
        }
    }

}