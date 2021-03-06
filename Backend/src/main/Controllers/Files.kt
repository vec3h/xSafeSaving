package Controllers

/**
 * Created by nikolaev on 03.09.16.
 */
import DB.createFile
import DB.deleteFile
import DB.getCountOfFiles
import DB.getUserFiles
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.jsonObject
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import spark.Request
import spark.Response
import java.io.File
import java.io.FileOutputStream
import java.net.InetAddress
import java.net.MalformedURLException
import java.net.URL
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*
import javax.servlet.MultipartConfigElement


fun getUserFiles(req: Request, res: Response): String {

    val obj: String
    res.status(200)

    val skip = req.queryParams("skip")
    val tag = req.queryParams("tag")

    val username : String? = req.session().attribute("user")
    if (username == null) {
        res.status(401)
        obj = JSONResponse.badJson()
        return obj
    }
    val result = getUserFiles(username, skip.toInt(), tag)

    return result
}

fun getPagination(req: Request, res: Response): String {
    val obj: String
    res.status(200)

    val username : String? = req.session().attribute("user")
    val tag = req.queryParams("tag")
    if (username == null) {
        res.status(401)
        obj = JSONResponse.authNeeded()
        return obj
    }

    val result = getCountOfFiles(username, tag)

    return result
}

fun deleteFile(req: Request, res: Response): String {
    val obj: String
    val gson = Gson()

    res.status(200)
    val username : String? = req.session().attribute("user")
    val list: Map<String, String>

    if (username == null) {
        res.status(401)
        obj = JSONResponse.authNeeded()
        return obj
    }
    try {
        list = gson.fromJson(req.body())
    } catch (e: com.google.gson.JsonSyntaxException) {
        res.status(400)
        obj = JSONResponse.badJson()
        return obj
    }
    val path : String? = list["path"]
    if (path != null) {
        deleteFile(username, path)
        res.status(200)
        obj = JSONResponse.makeCustomJsonResponse("message", "OK")
        return obj
    }

    res.status(403)
    obj = JSONResponse.makeCustomJsonResponse("message", "Some Error")
    return obj
}

fun uploadUserFiles(req: Request, res: Response): String {
    var obj: String
    val gson = Gson()

    res.status(200)
    val username : String? = req.session().attribute("user")
    val list: Map<String, String>

    if (username == null) {
        res.status(401)
        obj = JSONResponse.authNeeded()
        return obj
    }
    if (req.contentType() == "application/json") {
        try {
            list = gson.fromJson( req.body() )
        } catch (e: com.google.gson.JsonSyntaxException) {
            res.status(400)
            obj = JSONResponse.badJson()
            return obj
        }


        val url : String? = list["url"]
        if (url != null && InetAddress.getByName(URL(url).host).hostAddress != InetAddress.getLocalHost().hostAddress) {

            val size: Long
            val contentType: String
            val downloadURL: URL
            try {
                downloadURL = URL(url)
            } catch (e: MalformedURLException) {
                res.status(400)
                obj = JSONResponse.makeCustomJsonResponse("message", "Invalid URL")
                return obj
            }
            try {
                val conn = downloadURL.openConnection()
                size = conn.contentLengthLong
                contentType = conn.contentType
                conn.inputStream.close()
                if (size / (1024 * 1024) > 70) {
                    obj = JSONResponse.makeCustomJsonResponse("message", "File to big")
                    return obj
                }
            } catch (e: java.io.FileNotFoundException){
                res.status(400)
                obj = JSONResponse.makeCustomJsonResponse("message", "File from that url not found")
                return obj
            }
            val pathString = getDateAndCreateFolder(username)

            val rbc = Channels.newChannel(downloadURL.openStream())
            val randomFileName = UUID.randomUUID().toString().substring(0,6) + "." +
                    contentType.substring(contentType.lastIndexOf("/") + 1)
            val path = pathString + randomFileName
            val fos = FileOutputStream(path)
            fos.channel.transferFrom(rbc, 0, java.lang.Long.MAX_VALUE)
            createFile(username, path, randomFileName, contentType)

            val jsonArray = JsonArray()
            val innerObject = jsonObject()

            innerObject.addProperty("path", path)
            innerObject.addProperty("content-type", contentType)
            innerObject.addProperty("filename", randomFileName)

            jsonArray.add(innerObject)
            obj = gson.toJson(jsonArray)
            return obj
        } else {
            res.status(403)
            obj = JSONResponse.makeCustomJsonResponse("message", "Invalid URL")
            return obj
        }

    } else { //upload from multipart-form-data

        val jsonArray = JsonArray()
        val location = "files"          // the directory location where files will be stored (not used)
        val maxFileSize: Long = 100000000       // 100 mb for file
        val maxRequestSize: Long = 200000000    // 200 mb for all files
        val fileSizeThreshold = 1024

        val multipartConfigElement = MultipartConfigElement(
                location, maxFileSize, maxRequestSize, fileSizeThreshold)
        req.raw().setAttribute("org.eclipse.jetty.multipartConfig",
                multipartConfigElement)

        val parts = req.raw().parts
        val pathString = getDateAndCreateFolder(username)

        for (part in parts) {
            var filename = ""
            part.inputStream.use({ `in` ->
                    filename = part.submittedFileName.replace("/", "")
                    Files.copy(`in`, Paths.get(pathString + filename),
                            StandardCopyOption.REPLACE_EXISTING)

            })
            createFile(username, pathString + part.submittedFileName, filename,
                    part.contentType)

            val innerObject = JsonObject()
            innerObject.addProperty("path", pathString + part.submittedFileName)
            innerObject.addProperty("content-type", part.contentType)
            innerObject.addProperty("filename", part.submittedFileName)

            jsonArray.add(innerObject)

        }
        obj = gson.toJson(jsonArray)
    }
    return obj
}

fun getDateAndCreateFolder(username:String):String {
    val date = Date()
    val cal = Calendar.getInstance()
    cal.time = date
    val year = cal.get(Calendar.YEAR)
    val month = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH)
    val day = cal.get(Calendar.DAY_OF_MONTH)

    val userDir = File("../upload/$username/$year/$month/$day")
    userDir.mkdirs()
    return "../upload/$username/$year/$month/$day/"
}
