const functions = require("firebase-functions");
const { GoogleGenerativeAI } = require("@google/generative-ai");

// Initialize Gemini API (User needs to set this in Firebase config or use secret manager)
// For this example, we expect the API key to be in the 'gemini.key' config.
const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY || functions.config().gemini.key);

exports.generateStudyPlan = functions.https.onCall(async (data, context) => {
    // Check authentication
    if (!context.auth) {
        throw new functions.https.HttpsError(
            "unauthenticated",
            "The function must be called while authenticated."
        );
    }

    const { subjects, availableHoursPerDay, examDate, weakSubjects } = data;

    if (!subjects || !availableHoursPerDay || !examDate) {
        throw new functions.https.HttpsError(
            "invalid-argument",
            "Missing required fields: subjects, availableHoursPerDay, or examDate."
        );
    }

    const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" });

    const prompt = `
        Generate a structured study plan for the following subjects: ${subjects.join(", ")}.
        The student has ${availableHoursPerDay} hours per day.
        Exams are on ${examDate}.
        Focus more on weak subjects: ${weakSubjects.join(", ")}.
        
        Return ONLY a JSON array of objects with these fields:
        [
          {
            "date": "YYYY-MM-DD",
            "subject": "Subject Name",
            "topic": "Specific Topic to study",
            "duration": "Duration in hours/minutes (e.g., '2h')"
          }
        ]
        Do not include any markdown formatting like \`\`\`json or extra text.
    `;

    try {
        const result = await model.generateContent(prompt);
        const response = await result.response;
        let text = response.text().trim();
        
        // Clean up any potential markdown if Gemini still includes it
        if (text.startsWith("```")) {
            text = text.replace(/^```json/, "").replace(/```$/, "").trim();
        }

        const studyPlan = JSON.parse(text);
        return { plan: studyPlan };
    } catch (error) {
        console.error("Gemini Error:", error);
        throw new functions.https.HttpsError(
            "internal",
            "Failed to generate study plan: " + error.message
        );
    }
});
