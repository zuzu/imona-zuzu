#!/usr/local/bin/perl

# �ꗗ�̊Ǘ�������������X�N���v�g

$brdbackup = "brdbackup";

chdir "/usr/local/apache/htdocs/";

if (! -e "$brdbackup") {
	system("mkdir $brdbackup");
}

&backupbrd;

# brd6.txt �Ƀ��O���o��
system("perl ./makebrdflex.pl > brd6.txt");

sub backupbrd {
	for ($i = 8; $i >= 0; $i--) {
		for $j (2..6) {
			#print "./$brdbackup/brd$j" . "_" . $i . ".txt";
			if (-e "./$brdbackup/brd$j" . "_" . $i . ".txt") {
				#print "mv ./$brdbackup/brd$j" . "_" . $i . ".txt ./$brdbackup/brd$j" . "_" . ($i+1) . ".txt";
				system("mv ./$brdbackup/brd$j" . "_" . $i . ".txt ./$brdbackup/brd$j" . "_" . ($i+1) . ".txt");
			}
		}
	}
	for $j (2..6) {
		if (-e "./$brdbackup/brd$j.txt") {
			system("mv ./$brdbackup/brd$j.txt ./$brdbackup/brd$j" . "_0.txt");
		}
	}
	system("cp brd*.txt ./$brdbackup");
}
