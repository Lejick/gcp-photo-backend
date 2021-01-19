package org.education.photobackend

import com.fasterxml.jackson.core.type.WritableTypeId
import com.google.cloud.vision.v1.Feature
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.gcp.data.datastore.core.mapping.Entity
import org.springframework.cloud.gcp.data.datastore.repository.DatastoreRepository
import org.springframework.cloud.gcp.vision.CloudVisionTemplate
import org.springframework.context.ApplicationContext
import org.springframework.core.io.Resource
import org.springframework.core.io.WritableResource
import org.springframework.data.annotation.Id
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@SpringBootApplication
class PhotoBackendApplication

fun main(args: Array<String>) {
    runApplication<PhotoBackendApplication>(*args)
}

@RepositoryRestResource
interface PhotoRepository : DatastoreRepository<Photo, String>

@Entity
data class Photo(
        @Id
        val id: String? = null,
        val uri: String? = null,
        val label: String?
)

@RestController
class HelloController(
        private val photoRepository: PhotoRepository
) {

    @PostMapping("/photo")
    fun create(@RequestBody photo: Photo) {
        photoRepository.save(photo)
    }
}

@RestController
class UploadController(
        private val ctx: ApplicationContext,
        private val photoRepository: PhotoRepository,
        private val visionTemplate: CloudVisionTemplate

) {
    val bucket = "gs://emerald-agility-301910-photo/images"

    @PostMapping("/upload")
    fun upload(@RequestParam("file") file: MultipartFile): Photo {
        val id = UUID.randomUUID().toString()
        val uri = "$bucket/$id"
        val gcs = ctx.getResource(uri) as WritableResource
        file.inputStream.use { input ->
            gcs.outputStream.use { output ->
                input.copyTo(output)
            }
        }
      val resp=  visionTemplate.analyzeImage(file.resource, Feature.Type.LABEL_DETECTION)
        println(resp)
        val labels=resp.labelAnnotationsList.take(5).map { it.description }.joinToString(",")
        return photoRepository.save(Photo(id, "/$id", labels))
    }

    @GetMapping("/image/{id}")
    fun get(@PathVariable id: String): ResponseEntity<Resource> {
        val resource = ctx.getResource("$bucket/$id")
        return if (resource.exists()) {
            ResponseEntity.ok(resource)
        } else {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }
}