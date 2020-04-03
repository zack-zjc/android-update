
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Created by zack on 2019/2/18.
 * kotlin实现crashHandler
 */


class CrashHandler private constructor(var context:Context) :Thread.UncaughtExceptionHandler{

    companion object INSTANCE{

        @Volatile
        var mHandlerInstance : CrashHandler? = null

        fun initHandler(context:Context) {
            if(mHandlerInstance == null){
                synchronized(CrashHandler::class){
                    if (mHandlerInstance == null){
                        mHandlerInstance = CrashHandler(context)
                    }
                }
            }
        }

    }

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread?, e: Throwable?) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
            manager?.cancelAll()
            writeThrowableInfoFile( "/sdcard/log/error.log", e)
            Thread.sleep(2000)
        }catch (e:Exception){
            e.printStackTrace()
        }finally {
            e?.printStackTrace()
            killApp()
        }
    }

    /**
     * 将异常写入文件
     */
    private fun writeThrowableInfoFile(filePath:String,throwable:Throwable?){
        val file = File(filePath)
        if (file.isDirectory || throwable == null) return
        val dir = File(file.parent)
        if (!dir.exists() && !dir.mkdir()) return
        if (!file.exists() && !file.createNewFile()) return
        val sb = StringBuffer()
        sb.append("\n-----" + SimpleDateFormat("MM-dd HH:mm:ss.SSS",Locale.CHINESE).format(Date(System.currentTimeMillis())) + "------\n")
        val writer = StringWriter()
        val printWriter = PrintWriter(writer)
        throwable.printStackTrace(printWriter)
        var cause: Throwable? = throwable.cause
        while (cause != null) {
            cause.printStackTrace(printWriter)
            cause = cause.cause
        }
        printWriter.close()
        val result = writer.toString()
        sb.append(result)
        var fileOutputStream: FileOutputStream? = null
        try {
            fileOutputStream = FileOutputStream(file, true)
            fileOutputStream.write(sb.toString().toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (fileOutputStream != null) {
                fileOutputStream.close()
            }
        }
    }

    private fun killApp(){
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
        activityManager?.runningAppProcesses?.filter { it.uid == android.os.Process.myUid() }
                ?.forEach { android.os.Process.killProcess(it.pid) }
        System.exit(1)
    }

}