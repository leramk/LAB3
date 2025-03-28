package org.lr3;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import javax.sql.DataSource;
import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/data")
public class Controller {

    @Autowired
    private DataSource dataSource;

    // З захистом від інєкції

    @PostMapping("/execute")
    public String executeQuery(@RequestParam String query) {
        String result;
        try (Connection connection = dataSource.getConnection()) {
            result = parseAndExecuteCommand(connection, query);
        } catch (SQLException e) {
            result = "Помилка в базі даних: " + e.getMessage();
        } catch (Exception e) {
            result = "Помилка: " + e.getMessage();
        }
        return result;
    }

     //Без захисту від інєкції

//   @PostMapping("/execute")
//    public String executeQuery(@RequestParam String query) {
//        StringBuilder result = new StringBuilder();
//        try (Connection connection = dataSource.getConnection();
//             Statement statement = connection.createStatement()) {
//            if (!isAllowedQuery(query)) {
//                return "Запит заборонено!";
//            }
//            if (query.trim().toLowerCase().startsWith("select")) {
//                try (ResultSet resultSet = statement.executeQuery(query)) {
//                    int columnCount = resultSet.getMetaData().getColumnCount();
//                    while (resultSet.next()) {
//                        for (int i = 1; i <= columnCount; i++) {
//                            result.append(resultSet.getString(i)).append(" ");
//                        }
//                        result.append("<br>");
//                    }
//                }
//            } else {
//                int rowsAffected = statement.executeUpdate(query);
//                result.append("Рядків змінено: ").append(rowsAffected);
//            }
//        } catch (SQLException e) {
//            result.append("Помилка: ").append(e.getMessage());
//        }
//        return result.toString();
//    }
//    private boolean isAllowedQuery(String query) {
//        String upperQuery = query.trim().toUpperCase();
//        String[] forbidden = { "DROP", "ALTER", "TRUNCATE", "EXEC", "GRANT", "REVOKE" };
//
//        for (String word : forbidden) {
//            if (upperQuery.startsWith(word)) {
//                return false;
//            }
//        }
//        return true;
//    }

    private String parseAndExecuteCommand(Connection connection, String query) throws SQLException {
        Pattern insertPattern = Pattern.compile("insert (\\w+)\\((.*)\\)", Pattern.CASE_INSENSITIVE);
        Pattern deletePattern = Pattern.compile("delete (\\w+)\\((.*)\\)", Pattern.CASE_INSENSITIVE);
        Pattern updatePattern = Pattern.compile("update (\\w+)\\((.*)\\)", Pattern.CASE_INSENSITIVE);
        Pattern selectPattern = Pattern.compile("select (\\w+)", Pattern.CASE_INSENSITIVE);

        Matcher insertMatcher = insertPattern.matcher(query);
        Matcher deleteMatcher = deletePattern.matcher(query);
        Matcher updateMatcher = updatePattern.matcher(query);
        Matcher selectMatcher = selectPattern.matcher(query);

        if (insertMatcher.matches()) {
            return handleInsert(connection, insertMatcher.group(1), insertMatcher.group(2));
        } else if (deleteMatcher.matches()) {
            return handleDelete(connection, deleteMatcher.group(1), deleteMatcher.group(2));
        } else if (updateMatcher.matches()) {
            return handleUpdate(connection, updateMatcher.group(1), updateMatcher.group(2));
        } else if (selectMatcher.matches()) {
            return handleSelect(connection, selectMatcher.group(1));
        }
        return "Неправильна команда";
    }

    private String handleInsert(Connection connection, String tableName, String values) throws SQLException {
        String sql;
        PreparedStatement preparedStatement;

        switch (tableName.toLowerCase()) {
            case "judges":
                validateJudgeInput(values);
                sql = "INSERT INTO Judges (name, surname) VALUES (?, ?)";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, extractValue(values, "name"));
                preparedStatement.setString(2, extractValue(values, "surname"));
                break;

            case "cases":
                validateCaseInput(values);
                sql = "INSERT INTO Cases (case_number, judge_id) VALUES (?, ?)";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, extractValue(values, "case_number"));
                preparedStatement.setInt(2, Integer.parseInt(extractValue(values, "judge_id")));
                break;

            case "convicts":
                validateConvictInput(values);
                sql = "INSERT INTO Convicts (name, surname, description, case_id, sentence) VALUES (?, ?, ?, ?, ?)";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, extractValue(values, "name"));
                preparedStatement.setString(2, extractValue(values, "surname"));
                preparedStatement.setString(3, extractValue(values, "description"));
                preparedStatement.setInt(4, Integer.parseInt(extractValue(values, "case_id")));
                preparedStatement.setString(5, extractValue(values, "sentence"));
                break;

            default:
                return "Неправильне ім'я таблиці";
        }

        int rowsAffected = preparedStatement.executeUpdate();
        return "Додано " + rowsAffected + " рядок до " + tableName;
    }

    private String handleDelete(Connection connection, String tableName, String values) throws SQLException {
        String sql;
        PreparedStatement preparedStatement;

        switch (tableName.toLowerCase()) {
            case "judges":
                sql = "DELETE FROM Judges WHERE judge_id = ?";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setInt(1, Integer.parseInt(extractValue(values, "judge_id")));
                break;

            case "cases":
                sql = "DELETE FROM Cases WHERE case_id = ?";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setInt(1, Integer.parseInt(extractValue(values, "case_id")));
                break;

            case "convicts":
                sql = "DELETE FROM Convicts WHERE convict_id = ?";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setInt(1, Integer.parseInt(extractValue(values, "convict_id")));
                break;

            default:
                return "Неправильне ім'я таблиці";
        }
        int rowsAffected = preparedStatement.executeUpdate();
        return "Видалено " + rowsAffected + " рядок з " + tableName;
    }

    private String handleUpdate(Connection connection, String tableName, String values) throws SQLException {
        String sql;
        PreparedStatement preparedStatement;

        switch (tableName.toLowerCase()) {
            case "judges":
                validateJudgeInput(values);
                sql = "UPDATE Judges SET name = ?, surname = ? WHERE judge_id = ?";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, extractValue(values, "name"));
                preparedStatement.setString(2, extractValue(values, "surname"));
                preparedStatement.setInt(3, Integer.parseInt(extractValue(values, "judge_id")));
                break;

            case "cases":
                validateCaseInput(values);
                sql = "UPDATE Cases SET case_number = ?, judge_id = ? WHERE case_id = ?";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, extractValue(values, "case_number"));
                preparedStatement.setInt(2, Integer.parseInt(extractValue(values, "judge_id")));
                preparedStatement.setInt(3, Integer.parseInt(extractValue(values, "case_id")));
                break;

            case "convicts":
                validateConvictInput(values);
                sql = "UPDATE Convicts SET name = ?, surname = ?, description = ?, case_id = ?, sentence = ? WHERE convict_id = ?";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, extractValue(values, "name"));
                preparedStatement.setString(2, extractValue(values, "surname"));
                preparedStatement.setString(3, extractValue(values, "description"));
                preparedStatement.setInt(4, Integer.parseInt(extractValue(values, "case_id")));
                preparedStatement.setString(5, extractValue(values, "sentence"));
                preparedStatement.setInt(6, Integer.parseInt(extractValue(values, "convict_id")));
                break;

            default:
                return "Неправильне ім'я таблиці";
        }

        int rowsAffected = preparedStatement.executeUpdate();
        return "Оновлено " + rowsAffected + " рядок(ів) в " + tableName;
    }

    private String handleSelect(Connection connection, String tableName) throws SQLException {
        StringBuilder result = new StringBuilder();
        String sql = "SELECT * FROM " + tableName;

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                result.append(metaData.getColumnName(i)).append("\t");
            }
            result.append("<br>");

            while (resultSet.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    result.append(resultSet.getString(i)).append("\t");
                }
                result.append("<br>");
            }
        }

        return result.toString();
    }

    private String extractValue(String values, String fieldName) {
        Pattern fieldPattern = Pattern.compile(fieldName + "=\\'([^']+)\\'", Pattern.CASE_INSENSITIVE);
        Matcher fieldMatcher = fieldPattern.matcher(values);

        if (fieldMatcher.find()) {
            return fieldMatcher.group(1);
        }
        return "";
    }

    private void validateJudgeInput(String values) {
        String name = extractValue(values, "name");
        String surname = extractValue(values, "surname");

        if (!name.matches("[a-zA-Zа-яА-Я]+")) {
            throw new IllegalArgumentException("Ім'я судді має містити лише літери");
        }

        if (!surname.matches("[a-zA-Zа-яА-Я]+")) {
            throw new IllegalArgumentException("Прізвище судді має містити лише літери");
        }
    }

    private void validateCaseInput(String values) {
        String caseNumber = extractValue(values, "case_number");
        String judgeId = extractValue(values, "judge_id");

        if (!caseNumber.matches("[a-zA-Z0-9-]+")) {
            throw new IllegalArgumentException("Номер справи має містити лише літери, цифри та дефіси");
        }

        if (!judgeId.matches("\\d+")) {
            throw new IllegalArgumentException("ID судді має бути числом");
        }
    }

    private void validateConvictInput(String values) {
        String name = extractValue(values, "name");
        String surname = extractValue(values, "surname");
        String caseId = extractValue(values, "case_id");

        if (!name.matches("[a-zA-Zа-яА-Я]+")) {
            throw new IllegalArgumentException("Ім'я осужденного має містити лише літери");
        }

        if (!surname.matches("[a-zA-Zа-яА-Я]+")) {
            throw new IllegalArgumentException("Прізвище осужденного має містити лише літери");
        }

        if (!caseId.matches("\\d+")) {
            throw new IllegalArgumentException("ID справи має бути числом");
        }
    }
}