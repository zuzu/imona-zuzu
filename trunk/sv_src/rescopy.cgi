#!/usr/local/bin/perl

##�ݒ肱������##

$ver = '050412';#�o�[�W�����\�L(�X�V��)

$d = '../dat';#dat�t�H���_�̏ꏊ

$c = '10';#�����񐔂̏���l

##�ݒ肱���܂�##

require 'cgi-lib.pl';

&ReadParse(*input_data);

$server = $input_data{'server'};
$directory = $input_data{'directory'};
$datno = $input_data{'datno'};
$datno2 = $datno . '.dat';
$resno = $input_data{'resno'};
$inyou = $input_data{'inyou'};
$seikei = $input_data{'seikei'};

$inyou =~ s/&/&amp;/g;
$inyou =~ s/</&lt;/g;
$inyou =~ s/>/&gt;/g;


open(FILE,"$d/$directory/$datno2");
@dat = <FILE>;
close FILE;

print "Content-type: text/html;charset=Shift_JIS\n\n";
print "RC.ver$ver";
print "<HR>";
print "<form method=get action=rescopy.cgi>";
print "<INPUT type=hidden name=server value=$server>";
print "<INPUT type=hidden name=directory value=$directory>";
print "<INPUT type=hidden name=datno value=$datno>";
print "���p��<input type=text name=inyou size=2 value=$inyou>���g�p����<BR>";
print "<input type=text name=resno size=3>�Ԃ�";
print "<SELECT name=seikei>";
print "<OPTION value=1>�S��</OPTION>";
print "<OPTION value=2>�{��</OPTION>";
print "<OPTION value=3>�S���Q</OPTION>";
print "<OPTION value=4>�{���Q</OPTION>";
print "<OPTION value=5>�S���R</OPTION>";
print "<OPTION value=6>�{���R</OPTION>";
print "</SELECT>";
print "<input type=submit value=COPY></form>";
print "<HR>";

$str = $dat[0];
@txt = split(/\t/,$str);
$max_resno = $txt[1];
print "<FONT size=-1>�����ő僌�X�ԍ���$max_resno�ł��B</FONT><HR>";

$str = $dat[1];
@txt = split(/<>/,$str);
$title = $txt[4];
print "<FORM action=i><TEXTAREA>$title$server/test/read.cgi/$directory/$datno/</TEXTAREA></FORM>";
print "<FORM action=i><TEXTAREA>�ҏW�G���A</TEXTAREA></FORM>";

###################

@resno = split(/ |�@/,$resno);
$i = 0;
foreach $no (@resno){

$i++;#�����J�E���g
if($i > $c){
print "<FONT size=-1 color=#ff0000>�������ׂ�}����ׂ�$i�ڂŏ����𒆒f���܂����B</FONT><BR>";
last;
}

if($no =~ /[^0-9]/){#����m�F
$ii++;
next;
}

if($no > $max_resno){#���X���m�F
$iii++;
next;
}

if($no > 0){
$str = $dat[$no];

$str =~ s/ ?<br> ?/\n$inyou/gi;
$str =~ s/ ?<> ?/\t/gi;
$str =~ s/ ?<\/b> ?//gi;
$str =~ s/ ?<a \S* \S{16}? ?//gi;
$str =~ s/ ?<b> ?//gi;
$str =~ s/ ?<\/a> ?//gi;
@txt = split(/\t/,$str);

	if($seikei == 1){#�S��
	print "<FORM action=i><TEXTAREA>";
	print "$inyou$no ���O: $txt[0] [$txt[1]] ���e��: $txt[2]\n";
	print "$inyou$txt[3]\n";
	print "</TEXTAREA></FORM>";
	}
	elsif($seikei == 2){#�{��
	print "<FORM action=i><TEXTAREA>";
	print "$inyou$txt[3]\n";
	print "</TEXTAREA></FORM>";
	}
	elsif($seikei == 3){#�S���Q
	print "<FORM action=i><TEXTAREA>";
	print "$inyou$no ���O: $txt[0] [$txt[1]] ���e��: $txt[2]\n";
	print "$inyou$txt[3]\n";
	print "\n$server/test/read.cgi/$directory/$datno/$no";
	print "</TEXTAREA></FORM>";
	}
	elsif($seikei == 4){#�{���Q
	print "<FORM action=i><TEXTAREA>";
	print "$inyou$txt[3]\n";
	print "\n$server/test/read.cgi/$directory/$datno/$no";
	print "</TEXTAREA></FORM>";
	}
	elsif($seikei == 5){#�S���R
	print "<FORM action=i><TEXTAREA>";
	print "$inyou$no ���O: $txt[0] [$txt[1]] ���e��: $txt[2]\n";
	print "$inyou$txt[3]\n";
	print "\n$title$server/test/read.cgi/$directory/$datno/$no";
	print "</TEXTAREA></FORM>";
	}
	elsif($seikei == 6){#�{���R
	print "<FORM action=i><TEXTAREA>";
	print "$inyou$txt[3]\n";
	print "\n$title$server/test/read.cgi/$directory/$datno/$no";
	print "</TEXTAREA></FORM>";
	}

}
}

if($ii > 0){
print "<FONT size=-1 color=#ff0000>�������p�����ȊO�͏������܂���B($ii��)</FONT><BR>";
}

if($iii > 0){
print "<FONT size=-1 color=#ff0000>�����ő僌�X�ԍ��ȏ�͏������܂���B($iii��)</FONT><BR>";
}

print "<FONT size=-1>���������������܂����B</FONT><HR>";
#print "<a href=help.txt>HELP</a><BR><BR>";
print "��������l<BR>�W�̂P�U�W<BR>��iMona7m46s";
print "<BR>���ӌ��E���v�]��<a href=http://nov.s56.xrea.com/test/r.cgi/nov/1107780040/l10>������</a>��";
exit;
