#!/usr/bin/perl

#��ɽ��������ץ�

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
	print "from 02/08/1?";
	
} else {
	print "���顼<br>�����ä������⤷��ޤ���";
}

exit();