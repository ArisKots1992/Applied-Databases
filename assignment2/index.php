<!DOCTYPE html>
<html>
<body>

<form method="post" action="<?php echo $_SERVER['PHP_SELF'];?>">
   Search Text: <input type="text" name="searchText">
   <!-- Search Field: <input type="text" name="searchField"> -->
   <!-- <input type="submit"> -->
   <input type="submit" name="textSearch" alt="text search" value="Basic Search"/>
   <br>
   Width: <input type="text" name="width">  myLatitude: <input type="text" name="centerX"> myLongitude: <input type="text" name="centerY">
   <input type="submit" name="spatialSearch" alt="spatial search" value="Spatial Search"/>
</form>

<?php
if ($_SERVER["REQUEST_METHOD"] == "POST") {
  if (isset($_POST['textSearch'])){
    // collect value of input field
    $searchText = format_input($_POST['searchText']);
   //  $searchField = format_input($_POST['searchField']);

    if (empty($searchText)) {
        echo "Search text is empty";
    } else {
        $output = shell_exec("java -cp /usr/share/java/mysql-connector-java-5.1.28.jar:/usr/share/java/lucene-core-5.4.0.jar:/usr/share/java/lucene-analyzers-common-5.4.0.jar:/usr/share/java/lucene-queryparser-5.4.0.jar:/usr/share/java/lucene-queries-5.4.0.jar:. Searcher"." ".$searchText);
        //It is totally unsafe to do this in a real website, usually it is unacceptable to generate the shell command by user input, it is way too easy to hack. The only reason why we are doing so here is that we are pretty sure it will only be run locally and it is an easy way to run a java program from a web page without wrapping it.
        //echo $output;
        echo "Running <b>Basic Search</b>(" . $searchText . ")";
        echo "<style type=\"text/css\">
        .tftable {font-size:12px;color:#333333;width:100%;border-width: 1px;border-color: #729ea5;border-collapse: collapse;}
        .tftable th {font-size:12px;background-color:#acc8cc;border-width: 1px;padding: 8px;border-style: solid;border-color: #729ea5;text-align:left;}
        .tftable tr {background-color:#d4e3e5;}
        .tftable td {font-size:12px;border-width: 1px;padding: 8px;border-style: solid;border-color: #729ea5;}
        .tftable tr:hover {background-color:#ffffff;}
        </style>";
        echo "<table class=\"tftable\" border='1'>
                <tr>
                <th><span style=\"font-weight:bold\">ID</span></th>
                <th><span style=\"font-weight:bold\">Name</span></th>
                <th><span style=\"font-weight:bold\">Score</span></th>
                <th><span style=\"font-weight:bold\">Price</span></th>
                </tr>";
        $counter = 0;
        $hold="";
        foreach(preg_split("/((\r?\n)|(\r\n?))/", $output) as $line){
            $counter++;
            $line = explode("|", $line);
            if($counter==1)
                continue;
            if( count($line) == 1 && $counter > 1){
                $hold .= $line[0];
                continue;
            }
            echo "<tr>";
            echo "<td>" . $line[0] . "</td>";
            echo "<td>" . $line[1] . "</td>";
            echo "<td>" . $line[2] . "</td>";
            echo "<td>" . $line[3] . "</td>";
            echo "</tr>";
        }
        echo "</table><br>";
        echo $hold;

    }
  }
  elseif (isset($_POST['spatialSearch'])){
    $centerX = format_input($_POST['centerX']);
    $centerY = format_input($_POST['centerY']);
    $width = format_input($_POST['width']);
    $searchText = format_input($_POST['searchText']);

    if (empty($centerX)||empty($centerY)||empty($width)) {
        echo "width or myLatitude or myLongitude is empty";
    } else {
        $output = shell_exec("java -cp /usr/share/java/mysql-connector-java-5.1.28.jar:/usr/share/java/lucene-core-5.4.0.jar:/usr/share/java/lucene-analyzers-common-5.4.0.jar:/usr/share/java/lucene-queryparser-5.4.0.jar:/usr/share/java/lucene-queries-5.4.0.jar:. Searcher"." ".$searchText." -x ".$centerY." -y ".$centerX." -w ".$width);
        //It is totally unsafe to do this in a real website, usually it is unacceptable to generate the shell command by user input, it is way too easy to hack. The only reason why we are doing so here is that we are pretty sure it will only be run locally and it is an easy way to run a java program from a web page without wrapping it.
        echo "Running <b>Spatial Search</b>(" . $searchText . ") " ." -x ".$centerY." -y ".$centerX." -w ".$width;
        echo "<style type=\"text/css\">
        .tftable {font-size:12px;color:#333333;width:100%;border-width: 1px;border-color: #729ea5;border-collapse: collapse;}
        .tftable th {font-size:12px;background-color:#acc8cc;border-width: 1px;padding: 8px;border-style: solid;border-color: #729ea5;text-align:left;}
        .tftable tr {background-color:#d4e3e5;}
        .tftable td {font-size:12px;border-width: 1px;padding: 8px;border-style: solid;border-color: #729ea5;}
        .tftable tr:hover {background-color:#ffffff;}
        </style>";
        echo "<table class=\"tftable\" border='1'>
                <tr>
                <th><span style=\"font-weight:bold\">ID</span></th>
                <th><span style=\"font-weight:bold\">Name</span></th>
                <th><span style=\"font-weight:bold\">Score</span></th>
                <th><span style=\"font-weight:bold\">Price</span></th>
                <th><span style=\"font-weight:bold\">Distance</span></th>
                </tr>";
        $counter = 0;
        $hold="";
        foreach(preg_split("/((\r?\n)|(\r\n?))/", $output) as $line){
            $counter++;
            $line = explode("|", $line);
            if($counter==1)
                continue;
            if( count($line) == 1 && $counter > 1){
                $hold .= $line[0];
                continue;
            }
            echo "<tr>";
            echo "<td>" . $line[0] . "</td>";
            echo "<td>" . $line[1] . "</td>";
            echo "<td>" . $line[2] . "</td>";
            echo "<td>" . $line[3] . "</td>";
            echo "<td>" . $line[4] . "</td>";
            echo "</tr>";
        }
        echo "</table><br>";
        echo $hold;
    }
  }
}

function format_input($data) {
  $data = trim($data);
  $data = stripslashes($data);
  $data = htmlspecialchars($data);
  return $data;
}
?>

</body>
</html>

