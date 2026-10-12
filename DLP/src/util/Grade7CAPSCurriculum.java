package util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class Grade7CAPSCurriculum {

    private static final String MATHEMATICS = "Mathematics";
    private static final String ENGLISH_FAL = "English First Additional Language";
    private static final String GRADE = "Grade 7";

    private Grade7CAPSCurriculum() {
    }

    public static int seed(Connection connection) throws SQLException {
        String sql = "INSERT OR IGNORE INTO CAPSEntry "
            + "(capsCode, subject, gradeLevel, term, topic, outcomes, assessmentStandards) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        List<Topic> topics = topics();
        boolean originalAutoCommit = connection.getAutoCommit();
        int inserted = 0;

        try {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (Topic topic : topics) {
                    statement.setString(1, topic.code());
                    statement.setString(2, topic.subject());
                    statement.setString(3, GRADE);
                    statement.setInt(4, topic.term());
                    statement.setString(5, topic.name());
                    statement.setString(6, topic.outcomes());
                    statement.setString(7, topic.assessment());
                    inserted += statement.executeUpdate();
                }
            }
            connection.commit();
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }

        return inserted;
    }

    private static List<Topic> topics() {
        List<Topic> topics = new ArrayList<>();
        addMath(topics, 1, "Whole numbers",
            "Read, compare, order and calculate with whole numbers; use factors, multiples and prime factors.");
        addMath(topics, 1, "Integers",
            "Represent, compare and order integers, and solve problems using the four operations.");
        addMath(topics, 1, "Common fractions",
            "Compare and order fractions, find equivalent fractions, and calculate with common fractions.");
        addMath(topics, 1, "Numeric patterns",
            "Investigate, describe and extend numeric patterns, including rules expressed in words.");
        addMath(topics, 1, "Geometric patterns",
            "Recognise, describe and extend geometric patterns using diagrams and verbal rules.");
        addMath(topics, 1, "Algebraic expressions",
            "Use variables, terms and coefficients to write and simplify algebraic expressions.");
        addMath(topics, 1, "Geometry of 2-D shapes",
            "Identify, classify and describe triangles, quadrilaterals and other polygons by their properties.");
        addMath(topics, 1, "Geometric constructions",
            "Construct and measure angles and line segments using appropriate geometric instruments.");
        addMath(topics, 1, "Data handling: collect and organise data",
            "Pose questions, collect data, and organise it in suitable tables and tally charts.");

        addMath(topics, 2, "Common fractions: calculations",
            "Calculate with common fractions and solve contextual fraction problems.");
        addMath(topics, 2, "Decimal fractions",
            "Compare, order and calculate with decimal fractions, and use decimals in everyday problems.");
        addMath(topics, 2, "Percentages",
            "Convert between fractions, decimals and percentages, and calculate percentages of quantities.");
        addMath(topics, 2, "Ratio and rate",
            "Represent and simplify ratios, compare rates, and solve proportional-sharing problems.");
        addMath(topics, 2, "Functions and relationships",
            "Describe relationships between quantities using words, tables, flow diagrams and number sentences.");
        addMath(topics, 2, "Algebraic equations",
            "Solve simple equations by inspection and inverse operations, and check solutions by substitution.");
        addMath(topics, 2, "Geometry of 3-D objects",
            "Recognise and describe prisms and other 3-D objects by their faces, edges and vertices.");
        addMath(topics, 2, "Perimeter and area",
            "Calculate perimeter and area of polygons, including rectangles, triangles and composite shapes.");
        addMath(topics, 2, "Data handling: represent and interpret data",
            "Display data using appropriate graphs and interpret information from tables and graphs.");

        addMath(topics, 3, "Integers: calculations and problem solving",
            "Apply the four operations with integers and explain solutions to contextual problems.");
        addMath(topics, 3, "Exponents",
            "Use exponent notation, evaluate whole-number powers and apply the laws of exponents at an introductory level.");
        addMath(topics, 3, "Numeric patterns and rules",
            "Determine and describe rules for numeric patterns and use a rule to calculate later terms.");
        addMath(topics, 3, "Algebraic expressions: substitution and simplification",
            "Simplify expressions by collecting like terms and substitute values for variables.");
        addMath(topics, 3, "Algebraic equations: solve and verify",
            "Form, solve and verify simple equations arising from number sentences and word problems.");
        addMath(topics, 3, "Transformations",
            "Describe and perform translations, reflections and rotations of 2-D shapes on a grid.");
        addMath(topics, 3, "Surface area and volume",
            "Investigate and calculate surface area and volume of cubes and rectangular prisms using suitable units.");
        addMath(topics, 3, "Data handling: summarise and analyse data",
            "Calculate and interpret measures of central tendency and range for appropriate data sets.");

        addMath(topics, 4, "Whole numbers, fractions and decimals",
            "Consolidate calculations with whole numbers, common fractions and decimal fractions in multi-step problems.");
        addMath(topics, 4, "Percentages, ratio and rate",
            "Solve multi-step everyday problems involving percentages, ratios and rates.");
        addMath(topics, 4, "Functions, relationships and graphs",
            "Represent and interpret simple relationships using tables, rules and coordinate graphs.");
        addMath(topics, 4, "Geometry of 2-D shapes: angles and properties",
            "Use angle and side properties to classify and solve problems involving 2-D shapes.");
        addMath(topics, 4, "Measurement: perimeter, area and volume",
            "Select appropriate units and solve measurement problems involving perimeter, area and volume.");
        addMath(topics, 4, "Probability",
            "Describe the likelihood of outcomes and compare experimental and theoretical probabilities in simple cases.");
        addMath(topics, 4, "Data handling: interpret and report findings",
            "Interpret data displays, draw conclusions and communicate findings using appropriate mathematical language.");

        addEnglish(topics, 1, "Listening and speaking: instructions and conversation",
            "Listen for key information, follow and give instructions, and participate appropriately in conversations.");
        addEnglish(topics, 1, "Listening and speaking: prepared oral reading",
            "Read a short prepared text aloud with clear pronunciation, suitable pace and expression.");
        addEnglish(topics, 1, "Reading and viewing: short stories and visual texts",
            "Read age-appropriate stories and visual texts, identify main ideas and details, and support answers with evidence.");
        addEnglish(topics, 1, "Writing and presenting: paragraphs and narrative",
            "Plan, draft, edit and present a coherent descriptive or narrative paragraph using a clear sequence.");
        addEnglish(topics, 1, "Language structures and conventions: sentences and punctuation",
            "Use sentence types, capitalisation, end punctuation, nouns, pronouns and basic verb forms in context.");

        addEnglish(topics, 2, "Listening and speaking: recounts and discussions",
            "Listen to recounts and discussions, identify sequence and key points, and respond with relevant questions.");
        addEnglish(topics, 2, "Listening and speaking: dialogue and role-play",
            "Prepare and present a short dialogue or role-play using appropriate turn-taking and register.");
        addEnglish(topics, 2, "Reading and viewing: informational and poetic texts",
            "Read informational texts and poems, locate facts, infer meaning from context and discuss language choices.");
        addEnglish(topics, 2, "Writing and presenting: transactional texts",
            "Write and edit a short functional text, such as a friendly letter, email, invitation or set of directions.");
        addEnglish(topics, 2, "Language structures and conventions: word classes and tense",
            "Identify and use adjectives, adverbs, prepositions, conjunctions and suitable present and past verb tenses.");

        addEnglish(topics, 3, "Listening and speaking: stories and oral response",
            "Listen to a story or oral text, identify its key events and respond by retelling or expressing an opinion.");
        addEnglish(topics, 3, "Listening and speaking: prepared speech",
            "Plan and deliver a short prepared speech with a clear opening, organised points and audible delivery.");
        addEnglish(topics, 3, "Reading and viewing: literature and media",
            "Read a literary text and a media or visual text, distinguish fact from opinion and explain basic character or text features.");
        addEnglish(topics, 3, "Writing and presenting: summaries and reports",
            "Select relevant information and write a concise summary or short report in an appropriate format.");
        addEnglish(topics, 3, "Language structures and conventions: direct and reported speech",
            "Use direct and reported speech, subject-verb agreement, pronouns and punctuation in connected writing.");

        addEnglish(topics, 4, "Listening and speaking: presentations and discussion",
            "Listen critically to an oral presentation, identify its main message and contribute relevant ideas to a discussion.");
        addEnglish(topics, 4, "Listening and speaking: oral presentation",
            "Prepare and present a short oral presentation using supporting notes, appropriate vocabulary and clear delivery.");
        addEnglish(topics, 4, "Reading and viewing: comprehension and visual literacy",
            "Read age-appropriate fiction and non-fiction, infer meaning and interpret basic visual and textual features.");
        addEnglish(topics, 4, "Writing and presenting: essays and transactional writing",
            "Plan, draft, revise and edit a structured paragraph or short essay and a functional transactional text.");
        addEnglish(topics, 4, "Language structures and conventions: revision and editing",
            "Apply spelling, punctuation, sentence construction, agreement and familiar verb tenses when editing own writing.");

        return List.copyOf(topics);
    }

    private static void addMath(List<Topic> topics, int term, String name, String outcomes) {
        add(topics, MATHEMATICS, term, name, outcomes);
    }

    private static void addEnglish(List<Topic> topics, int term, String name, String outcomes) {
        add(topics, ENGLISH_FAL, term, name, outcomes);
    }

    private static void add(List<Topic> topics, String subject, int term, String name, String outcomes) {
        String subjectCode = subject.equals(MATHEMATICS) ? "MATH" : "ENFAL";
        String code = "CAPS-G7-" + subjectCode + "-T" + term + "-"
            + String.format("%02d", topics.stream()
                .filter(topic -> topic.subject().equals(subject) && topic.term() == term)
                .count() + 1);
        String assessment = subject.equals(MATHEMATICS)
            ? "Assess through worked calculations, written explanations and contextual problem solving."
            : "Assess through listening or speaking activities, comprehension and a short written task as appropriate.";
        topics.add(new Topic(code, subject, term, name, outcomes, assessment));
    }

    private record Topic(
        String code,
        String subject,
        int term,
        String name,
        String outcomes,
        String assessment
    ) {
    }
}
