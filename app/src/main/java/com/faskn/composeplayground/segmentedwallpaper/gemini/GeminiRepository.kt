package com.faskn.composeplayground.segmentedwallpaper.gemini

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.asImageOrNull
import com.google.firebase.ai.type.content

class GeminiRepository {

    suspend fun generativeFill(
        image: Bitmap,
        prompt: String
    ): Result<Bitmap> {
        return try {
            Log.d("GeminiRepository", "Starting generative fill with Vertex AI")

            // Initialize Vertex AI with image generation model
            // The model will generate images based on input dimensions
            val generativeModel = Firebase.ai(backend = GenerativeBackend.vertexAI())
                .generativeModel(modelName = "gemini-2.0-flash-preview-image-generation")

            // Create content with image and prompt
            // Request output at same dimensions as input
            val enhancedPrompt =
                "$prompt Generate the output at the same dimensions (${image.width}x${image.height}) as the input image."

            val inputContent = content {
                text(enhancedPrompt)
                image(image)
            }

            Log.d(
                "GeminiRepository",
                "Sending request to Vertex AI with image ${image.width}x${image.height}..."
            )

            // Generate content
            val response = generativeModel.generateContent(inputContent)

            Log.d("GeminiRepository", "Response received from Vertex AI")

            // Try to get image from response
            val imagePart = response.candidates.firstOrNull()
                ?.content?.parts?.firstOrNull { it.asImageOrNull() != null }

            if (imagePart != null) {
                val resultImage = imagePart.asImageOrNull()!!
                Log.d(
                    "GeminiRepository",
                    "Image found in response: ${resultImage.width}x${resultImage.height}"
                )
                Result.success(resultImage)
            } else {
                val textResponse = response.text
                Log.e("GeminiRepository", "No image in response. Text: $textResponse")
                Result.failure(
                    Exception("API did not return an image. Response: ${textResponse?.take(200)}")
                )
            }
        } catch (e: Exception) {
            Log.e("GeminiRepository", "Error in generativeFill", e)
            Result.failure(Exception("Firebase AI Error: ${e.message}", e))
        }
    }
}

