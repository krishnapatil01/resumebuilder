package com.autoresume.service;

import com.autoresume.model.Resume;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AnalysisService {

    public String extractText(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) return "";
        
        if (filename.toLowerCase().endsWith(".pdf")) {
            try (PDDocument document = PDDocument.load(file.getInputStream())) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        } else if (filename.toLowerCase().endsWith(".docx")) {
            try (XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
                XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
                return extractor.getText();
            }
        }
        return "";
    }

    public Map<String, Object> analyzeFit(String resumeText, String jdText) {
        if (resumeText == null) resumeText = "";
        if (jdText == null) jdText = "";

        // Normalize text for deterministic results
        String normalizedResume = resumeText.toLowerCase().trim();
        String normalizedJd = jdText.toLowerCase().trim();

        Set<String> resumeKeywords = extractKeywords(normalizedResume);
        Set<String> jdKeywords = extractKeywords(normalizedJd);

        // If JD is very short (like just a job title), expand it with common industry keywords
        if (jdKeywords.size() < 5 && !normalizedJd.isEmpty()) {
            jdKeywords.addAll(getIndustryKeywords(normalizedJd));
        }

        // 1. Keyword Relevance (40%)
        double keywordScore = 0;
        Set<String> matched = new HashSet<>(jdKeywords);
        matched.retainAll(resumeKeywords);
        Set<String> missing = new HashSet<>(jdKeywords);
        missing.removeAll(matched);
        if (!jdKeywords.isEmpty()) {
            keywordScore = (double) matched.size() / jdKeywords.size() * 40.0;
        }

        // 2. Technical Competency (20%)
        long techMatches = matched.stream().filter(this::isTechnicalSkill).count();
        double skillsScore = Math.min(20.0, techMatches * 4.0);

        // 3. Work Experience & Impact (15%) - Sensitive to metrics
        long quantifiedCount = countOccurrences(normalizedResume, "(\\d+%)|(\\$\\d+)|(\\d+\\+)|(\\d+ (users|clients|customers|employees|projects|percent|percentile|million|billion|k))");
        double experienceScore = Math.min(15.0, quantifiedCount * 3.0);
        
        // 4. Education (10%)
        double educationScore = (containsAny(normalizedResume, "degree", "bachelor", "master", "phd", "university")) ? 10.0 : 0.0;

        // 5. Action Verbs (5%)
        long actionVerbsFound = countActionVerbs(normalizedResume);
        double actionVerbScore = Math.min(5.0, actionVerbsFound * 1.0);

        // 6. Formatting & ATS Readability (5%)
        double formattingScore = containsAny(normalizedResume, "experience", "skills", "education") ? 5.0 : 0.0;

        // 7. Consistency & Completeness (5%)
        double completenessScore = 0;
        if (normalizedResume.matches(".*[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}.*")) completenessScore += 2.5;
        if (normalizedResume.contains("linkedin.com") || normalizedResume.contains("github.com")) completenessScore += 2.5;

        double totalScore = keywordScore + skillsScore + experienceScore + educationScore + actionVerbScore + formattingScore + completenessScore;
        totalScore = Math.round(totalScore * 10.0) / 10.0;

        List<String> suggestions = generateSuggestions(normalizedResume, missing, matched, actionVerbsFound, quantifiedCount, formattingScore > 0, normalizedJd);

        Map<String, Object> result = new HashMap<>();
        result.put("fitScore", totalScore);
        result.put("matchedKeywords", matched.stream().sorted().collect(Collectors.toList()));
        result.put("missingSkills", missing.stream().sorted().limit(15).collect(Collectors.toList()));
        
        Map<String, Double> breakdown = new LinkedHashMap<>();
        breakdown.put("Keyword Relevance", Math.round(keywordScore * 10.0) / 10.0);
        breakdown.put("Technical Competency", Math.round(skillsScore * 10.0) / 10.0);
        breakdown.put("Work Impact", Math.round(experienceScore * 10.0) / 10.0);
        breakdown.put("Education", Math.round(educationScore * 10.0) / 10.0);
        breakdown.put("Action Verbs", Math.round(actionVerbScore * 10.0) / 10.0);
        breakdown.put("ATS Readability", Math.round(formattingScore * 10.0) / 10.0);
        breakdown.put("Completeness", Math.round(completenessScore * 10.0) / 10.0);
        
        result.put("breakdown", breakdown);
        result.put("suggestions", suggestions);
        result.put("summary", generateSummary(totalScore, matched.size(), missing.size()));

        return result;
    }

    private Set<String> getIndustryKeywords(String title) {
        title = title.toLowerCase();
        Set<String> extra = new HashSet<>();
        if (title.contains("developer") || title.contains("engineer") || title.contains("software")) {
            extra.addAll(Arrays.asList("api", "git", "agile", "sql", "testing", "cloud", "architecture", "cicd", "database", "security"));
        }
        if (title.contains("backend")) {
            extra.addAll(Arrays.asList("java", "python", "node", "spring", "microservices", "docker", "rest", "nosql", "redis"));
        }
        if (title.contains("frontend") || title.contains("web")) {
            extra.addAll(Arrays.asList("javascript", "react", "html", "css", "typescript", "angular", "vue", "responsive", "ux"));
        }
        if (title.contains("data")) {
            extra.addAll(Arrays.asList("analysis", "python", "r", "machine", "learning", "tableau", "visualization", "hadoop", "spark"));
        }
        if (title.contains("manager") || title.contains("lead")) {
            extra.addAll(Arrays.asList("leadership", "strategy", "project", "stakeholder", "budget", "team", "planning", "delivery"));
        }
        return extra;
    }

    private boolean isTechnicalSkill(String word) {
        List<String> techIndicators = Arrays.asList(
            "java", "python", "javascript", "react", "spring", "aws", "cloud", "api", "database", "sql", "git", "docker", "kubernetes",
            "html", "css", "nodejs", "c++", "c#", "ruby", "php", "typescript", "angular", "vue", "management", "leadership", "agile",
            "microservices", "rest", "graphql", "nosql", "mongodb", "postgresql", "oracle", "jenkins", "terraform", "ansible", "azure", "gcp"
        );
        return techIndicators.contains(word.toLowerCase());
    }

    private long countOccurrences(String text, String regex) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(text);
        long count = 0;
        while (matcher.find()) count++;
        return count;
    }

    private long countActionVerbs(String text) {
        String[] actionVerbs = {
            "led", "managed", "developed", "increased", "decreased", "saved", "accelerated", "implemented", "created", 
            "designed", "streamlined", "optimized", "spearheaded", "produced", "negotiated", "achieved", "exceeded", 
            "launched", "generated", "mentored", "trained", "collaborated", "distributed", "formulated", "initiated",
            "architected", "coordinated", "resolved", "executed", "pioneered", "enhanced"
        };
        String lowerText = text.toLowerCase();
        return Arrays.stream(actionVerbs).filter(v -> lowerText.contains(v)).count();
    }

    private List<String> generateSuggestions(String text, Set<String> missing, Set<String> matched, long actionVerbs, long quantified, boolean hasHeaders, String jdText) {
        List<String> suggestions = new ArrayList<>();
        String title = jdText.toLowerCase();
        
        if (!missing.isEmpty()) {
            String keywordsToAdd = missing.stream()
                    .limit(6)
                    .collect(Collectors.joining(", "));
            suggestions.add("<b>Keyword Alignment:</b> Missing crucial ATS terms like [" + keywordsToAdd + "]. We recommend weaving these directly into your professional experience summaries instead of arbitrarily listing them.");
        }
        
        if (quantified < 4) {
            suggestions.add("<b>Quantify Impact:</b> Modern ATS algorithms score resumes higher when achievements have measurable data. Use metrics (e.g., %, $, volume, time saved) like 'Boosted efficiency by 25%' rather than simply 'Improved efficiency'.");
        }
        
        if (actionVerbs < 8) {
            suggestions.add("<b>Stronger Action Verbs:</b> Move away from passive phrasing. Start your bullet points with dynamic verbs like 'Architected', 'Spearheaded', 'Optimized', or 'Designed'.");
        }

        // Tailored role suggestions
        if (title.contains("data engineer") || title.contains("data")) {
            suggestions.add("<b>Data Engineering Pro-Tip:</b> Ensure you clearly state the scale of data you have manipulated (e.g., 'Processed 5TB of log data daily') and explicitly name your ETL orchestration tools (like Airflow or dbt).");
        } else if (title.contains("java") || title.contains("spring") || title.contains("j2ee")) {
            suggestions.add("<b>Java Ecosystem Focus:</b> Hiring managers look for testing and CI/CD competency. Make sure to specify your experience with JUnit, Mockito, Jenkins, or GitHub Actions if you have it.");
        } else if (title.contains("python") || title.contains("backend") || title.contains("django")) {
            suggestions.add("<b>Backend Architecture:</b> Highlight your experience with building RESTful APIs or microservices, and ensure your database optimization expertise (SQL vs NoSQL trade-offs) is clear.");
        } else if (title.contains("cybersecurity") || title.contains("security") || title.contains("infosec")) {
            suggestions.add("<b>Security Expertise:</b> Mention standard compliance frameworks (ISO, SOC2, NIST) you are familiar with, and highlight specific vulnerability scanning or penetration testing methodologies.");
        } else if (title.contains("frontend") || title.contains("mern") || title.contains("react") || title.contains("node")) {
            suggestions.add("<b>Frontend & Fullstack Optimization:</b> Mention state management patterns, application performance metrics (like Core Web Vitals), and your approach to responsive, accessible design.");
        }

        if (!hasHeaders) {
            suggestions.add("<b>System Parsability:</b> ATS systems look for standard markers for sections. Keep names traditional: 'Experience', 'Education', 'Skills'.");
        }
        
        if (!text.toLowerCase().contains("certification") && !text.toLowerCase().contains("certificate")) {
            suggestions.add("<b>Skill Validation:</b> Certifications often act as hard filters for ATS mapping. If you have valid vendor certifications (e.g., AWS, Oracle, CompTIA), ensure they are in a dedicated 'Certifications' block.");
        }

        if (!text.toLowerCase().contains("linkedin.com") && !text.toLowerCase().contains("github.com")) {
            suggestions.add("<b>Digital Footprint:</b> Add your LinkedIn or GitHub profile URL. ATS tools often crawl these links to verify your digital footprint.");
        }

        return suggestions;
    }

    private String generateSummary(double score, int matchedCount, int missingCount) {
        if (score > 85) return "Excellent match! Your resume is highly optimized for this role.";
        if (score > 70) return "Good match. Minor optimizations in keywords and metrics could push you to the top tier.";
        if (score > 50) return "Moderate match. Focus on adding missing keywords and quantifying your achievements.";
        return "Low match. Significant restructuring needed to align with the job requirements.";
    }

    private boolean containsAny(String text, String... keywords) {
        String lowerText = text.toLowerCase();
        return Arrays.stream(keywords).anyMatch(k -> lowerText.contains(k.toLowerCase()));
    }

    private Set<String> extractKeywords(String text) {
        if (text == null || text.isEmpty()) return new HashSet<>();
        // Split by non-alphanumeric except for special tech chars like # and +
        String[] words = text.toLowerCase().replaceAll("[^a-z0-9#+]", " ").split("\\s+");
        Set<String> stopWords = new HashSet<>(Arrays.asList(
            "the", "and", "a", "or", "in", "to", "for", "with", "is", "at", "of", "on", "from", "by", "an", "be", "as", "it", 
            "this", "that", "was", "were", "using", "used", "working", "knowledge", "strong", "experience", "ability",
            "excellent", "highly", "motivated", "skills", "team", "responsibilities", "responsible", "proven", "proficient",
            "familiar", "etc", "i", "we", "my", "our", "their", "will", "would", "your", "can", "have", "has", "had", "which"
        ));
        
        return Arrays.stream(words)
                .filter(w -> w.length() > 2 && !stopWords.contains(w))
                .collect(Collectors.toSet());
    }
}
