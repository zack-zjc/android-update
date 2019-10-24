
import android.content.ContentValues
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.support.annotation.RequiresApi
import java.io.*

/**
 * @Author zack
 * @Date 2019/10/24
 * @Description 文件相关操作
 * @Version 1.0
 */
object FileUtil {

    /**
     * 获取媒体文件的路径
     * image,video,audio同理
     * @param imageFileId 图片数据库中对应文件的id
     * Android Q 公有目录只能通过Content Uri + id的方式访问，以前的File路径全部无效
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun queryImageFilePath(imageFileId:String):String{
        return MediaStore.Images.Media
            .EXTERNAL_CONTENT_URI
            .buildUpon()
            .appendPath(imageFileId).build().toString()
    }

    /**
     * 保存apk文件
     * 保存图片等其他文件一样如下处理
     * values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
     * values.put(MediaStore.Images.Media.TITLE, "Image.png");
     * values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + saveDirName);
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveToAndroidQ(context: Context, sourceFilePath:String, savedFileName:String):String?{
        val values = ContentValues()
        values.put(MediaStore.Downloads.DISPLAY_NAME, savedFileName)
        values.put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
        values.put(MediaStore.Downloads.RELATIVE_PATH, "Download/")
        val external = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val resolver = context.contentResolver
        val insertUri = resolver.insert(external, values) ?: return null
        val mFilePath = insertUri.toString()
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        try {
            outputStream = resolver.openOutputStream(insertUri)
            if (outputStream == null) return null
            var read: Int
            val sourceFile = File(sourceFilePath)
            if (sourceFile.exists()) { // 文件存在时
                inputStream = FileInputStream(sourceFile) // 读入原文件
                val buffer = ByteArray(1444)
                read = inputStream.read(buffer)
                while (read != -1) {
                    outputStream.write(buffer, 0, read)
                    read = inputStream.read(buffer)
                }
                outputStream.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
        return mFilePath
    }

    /**
     * 判断公有目录文件是否存在
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun isAndroidQFileExists(context: Context, filePath:String):Boolean{
        var assetFileDescriptor: AssetFileDescriptor? = null
        val contentResolver = context.contentResolver
        try {
            assetFileDescriptor = contentResolver.openAssetFileDescriptor(Uri.parse(filePath), "r")
            return assetFileDescriptor != null
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } finally {
            assetFileDescriptor?.close()
        }
        return false
    }

}