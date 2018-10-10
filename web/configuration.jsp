<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">
<%@ page import="java.sql.*" %>
<html>
<head>
    <style>
        table {
            padding-top: 5%;
            padding-left: 5%
        }

        .addit {
            padding-top: 5%;
            padding-left: 5%
        }
    </style>
    <script src="//ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js"></script>
    <title>display data from the table using jsp</title>
    <link rel="stylesheet" href="https://fonts.googleapis.com/icon?family=Material+Icons">
    <link rel="stylesheet" href="https://code.getmdl.io/1.3.0/material.indigo-blue.min.css">
    <script defer src="https://code.getmdl.io/1.3.0/material.min.js"></script>
    <script>
        $(document).ready(function () {
            var next = 1;
            $(".add-more").click(function (e) {
                e.preventDefault();
                var addto = "#field" + next;
                var addRemove = "#field" + (next);
                next = next + 1;
                var newIn = '<input autocomplete="off" class="input form-control" id="field' + next + '" name="field" type="text"  placeholder="Enter URL">';
                var newInput = $(newIn);
                var removeBtn = '<button id="remove' + (next - 1) + '" class="btn btn-danger remove-btn" >-</button></div><div id="field">';
                var removeButton = $(removeBtn);
                $(addto).after(newInput);
                $(addRemove).after(removeButton);
                $("#field" + next).attr('data-source', $(addto).attr('data-source'));
                $("#count").val(next);
                $('.remove-btn').click(function (e) {
                    e.preventDefault();
                    var fieldNum = this.id.charAt(this.id.length - 1);
                    var fieldID = "#field" + fieldNum;
                    $(this).remove();
                    $(fieldID).remove();
                });
            });
        });
    </script>
</head>
<body>
<form action="config" method="get" id="servletclass">
    <div class="addit">
        <div id="field">
            <input autocomplete="off" class="input" id="field1" name="field" type="text"
                   placeholder="Enter URL"/>
            <button id="b1" class="btn add-more" type="button">+</button>
        </div>
        <br>
        <div style="padding-left: 01%">
            <input type="submit"
                   class="mdl-button mdl-js-button mdl-button--raised mdl-js-ripple-effect mdl-button--accent"
                   value="add" id="add" name="add">
            </input>
            <input type="button"
                   class="mdl-button mdl-js-button mdl-button--raised mdl-js-ripple-effect mdl-button--accent"
                   onclick="window.location.href='meta.html'" value="CANCEL" id="Cancel" name="cancel">
            </input>
        </div>
    </div>
    <br><br><br><br>
    <%
        try {
            String connectionURL = "jdbc:postgresql://localhost:5432/postgres";
            Connection connection;
            Statement statement;
            ResultSet rs;
            Class.forName("org.postgresql.Driver").newInstance();
            connection = DriverManager.getConnection(connectionURL, "postgres", "postgres");
            statement = connection.createStatement();
            String QueryString = "SELECT metaurl,state FROM metaurls";
            rs = statement.executeQuery(QueryString);
    %>
    <TABLE cellpadding="15">
        <thead>
        <tr>
            <td><b>URLs</b></td>
            <td><b>State Change</b></td>
            <td><b>Remove URL</b></td>
        </tr>
        </thead>
        <tbody>
        <% int i = 1;
            while (rs.next()) { %>
        <tr>
            <td>
                <label id="url<%=i%>" name="url"><%=rs.getString(1)%>
                </label>
                <input type="hidden" value="<%=rs.getString(1)%>" name="url<%=i%>">
            </td>
            <td>
                <%if (rs.getInt(2) == 0) {%>
                <input type="submit"
                       class="mdl-button mdl-js-button mdl-button--raised mdl-js-ripple-effect mdl-button--accent"
                       value="activate" id="status<%=i%>" name="status<%=i%>"><%}%>
                <%if (rs.getInt(2) == 1) {%>
                <input type="submit"
                       class="mdl-button mdl-js-button mdl-button--raised mdl-js-ripple-effect mdl-button--accent"
                       value="deactivate" id="status<%=i%>" name="status<%=i%>"><%}%>
            </td>
            <td>
                <input type="submit"
                       class="mdl-button mdl-js-button mdl-button--raised mdl-js-ripple-effect mdl-button--accent"
                       value="remove" id="remove<%=i%>" name="remove<%=i%>">
                </input>
            </td>
        </tr>
        </tbody>
        <% i++;
        } %>
        <% rs.close();
            statement.close();
            connection.close();
        } catch (Exception ex) {
            System.out.println("abcjasdkcbj");
        }
        %>
    </TABLE>
</form>
</body>
</html>