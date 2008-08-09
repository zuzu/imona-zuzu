#!/usr/local/bin/perl

#ログ表示スクリプト

print "Content-type: text/html\n\n";

if(open(DATA, "./iMonaLog.txt")){
	$line = <DATA>;
	close(DATA);

	@log = split(/\t/,$line);

	print "iMona Access Counter<br>";
	print "[Total: " . ($log[0] + $log[1] + $log[2] + $log[3]) . "]<br>";
	print "ezplus: $log[0]<br>";
	print "iappli: $log[1]<br>";
	print "JAVAappli: $log[2]<br>";
	print "others: $log[3]<br>";
	$lastmodified = (stat "./iMonaLog.txt")[9];
	$lasttime = localtime($lastmodified);
	print "from $lasttime";
	
} else {
	print "エラー<br>ログが消えたかもしれません。";
}

exit();