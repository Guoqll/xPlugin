package com.frame.plugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.apache.commons.io.IOUtils
import java.io.*


object PluginUtil {

    private val CreateTitle = "NewMvvmGroup"
    private val CreateTip = "please input view name"
    private val ResLimitation = "x"//资源限定符


    fun getClassName(project: Project?): String? {
        val className = Messages.showInputDialog(project, CreateTip, CreateTitle, Messages.getQuestionIcon())
        return className
    }

    fun getResLimitation() = ResLimitation

    fun getResLimitationUpCase(): String {
        return ResLimitation.replaceFirstChar {
            it.uppercaseChar()
        }
    }

    public fun readAndroidManfiset(main: String) {
        //TODO:读取本地AndroidManifest.xml 文件失败
        /*val androidManifestPath = "${main}AndroidManifest.xml"
        val manifestStr = readFileManifest(androidManifestPath)
        println("manifestStr===$manifestStr")
        writetoFile(manifestStr + "test", main, "AndroidManifest.xml")*/
    }

    fun readFileManifest(filename: String): String? {
        var content = ""
        try {
            var inputStream: InputStream? = null
            inputStream = this.javaClass.getResourceAsStream(filename)
            //content = String(readStream(inputStream)!!)
            val writer = StringWriter()
            IOUtils.copy(inputStream, writer, "UTF-8")
            content = writer.toString()
        } catch (e: Exception) {
            println("error:${e.message}")
        }
        return content
    }

    public fun readFile(filename: String): String? {
        var content = ""
        try {
            var inputStream: InputStream? = null
            inputStream = this.javaClass.getResourceAsStream("/code/$filename")
            //content = String(readStream(inputStream)!!)
            val writer = StringWriter()
            IOUtils.copy(inputStream, writer, "UTF-8")
            content = writer.toString()
        } catch (e: Exception) {
            println("error:${e.message}")
        }
        return content
    }

    public fun writetoFile(content: String, filepath: String, filename: String) {
        try {
            val floder = File(filepath)
            // if file doesnt exists, then create it
            if (!floder.exists()) {
                floder.mkdirs()
            }
            val file = File("$filepath/$filename")
            if (!file.exists()) {
                file.createNewFile()
            }
            /*val fw = FileWriter(file.absoluteFile)
            val bw = BufferedWriter(fw)*/
            val bw = BufferedWriter(OutputStreamWriter(FileOutputStream(file.absoluteFile, true), "UTF-8"))
            bw.write(content)
            bw.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(Exception::class)
    public fun readStream(inStream: InputStream?): ByteArray? {
        val outSteam = ByteArrayOutputStream()
        try {
            val buffer = ByteArray(1024)
            var len = -1
            while (inStream!!.read(buffer).also { len = it } != -1) {
                outSteam.write(buffer, 0, len)
                println(String(buffer))
            }
        } catch (e: IOException) {
        } finally {
            outSteam.close()
            inStream!!.close()
        }
        return outSteam.toByteArray()
    }

    fun replaceActivityCode(activityCode: String, packageName: String, className: String): String {
        return activityCode.replace("&package&", packageName)
            .replace("&SimpleActivity&", "${className}Activity")
            .replace("&ActivitySplashBinding&", "${getResLimitationUpCase()}Activity${className}Binding")
            .replace("&ActivityViewBinding&", "${getResLimitationUpCase()}Activity${className}Binding")
            .replace("&SimpleViewModel&", "${className}ViewModel")
            .replace("&R.layout.activity_simple&", "R.layout.${getResLimitation()}_activity_${className.toLowerCase()}")
            .replace("&SimpleViewModel&", "${className}ViewModel")
            .replace("&SimpleIntent&", "${className}Intent")
            .replace("&SimpleState&", "${className}State")
    }

    fun replaceFragmentCode(activityCode: String, packageName: String, className: String): String {
        return activityCode.replace("&package&", packageName)
            .replace("&SimpleFragment&", "${className}Fragment")
            .replace("&ActivitySplashBinding&", "${getResLimitationUpCase()}Activity${className}Binding")
            .replace("&FragmentViewBinding&", "${getResLimitationUpCase()}Fragment${className}Binding")
            .replace("&SimpleViewModel&", "${className}ViewModel")
            .replace("&SimpleIntent&", "${className}Intent")
            .replace("&SimpleState&", "${className}State")
            .replace("&R.layout.fragment_simple&", "R.layout.${getResLimitation()}_fragment_${className.toLowerCase()}")
    }

    fun replaceViewModel(vmCode: String, packageName: String, className: String): String {
        return vmCode.replace("&package&", packageName)
            .replace("&SimpleViewModel&", "${className}ViewModel")
            .replace("&SimpleIntent&", "${className}Intent")
            .replace("&SimpleState&", "${className}State")
    }

    fun replaceIntent(intentCode: String, packageName: String, className: String): String {
        return intentCode.replace("&package&", packageName)
            .replace("&SimpleIntent&", "${className}Intent")
            .replace("&SimpleState&", "${className}State")
    }


}