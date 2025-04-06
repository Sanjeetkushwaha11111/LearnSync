package com.example.learnsync.quiz.api

data class OpenAiRequest(
    val model: String = "claude-3-5-sonnet",
    val messages: List<Message>
)

data class Message(
    val role: String,
    val content: String
)

data class OpenAiResponse(
    val id: String,
    val choices: List<Choice>
)

data class Choice(
    val message: Message,
    val finishReason: String
)

data class QuizGenerationRequest(
    val topic: String,
    val numberOfQuestions: Int = 5
)

fun createQuizPrompt(request: QuizGenerationRequest): String {
    return """
        Generate ${request.numberOfQuestions} multiple choice questions about ${request.topic}.
        Format the response as a JSON array of questions where each question has:
        - question: the question text
        - options: array of 4 possible answers
        - correctAnswer: index (0-3) of the correct answer
        - timeInSeconds: time when to show the question (10, 20, 30, 40, 50 seconds)
        
        Example format:
        {
          "questions": [
            {
              "question": "What is...",
              "options": ["A", "B", "C", "D"],
              "correctAnswer": 0,
              "timeInSeconds": 10
            }
          ]
        }
    """.trimIndent()
}
