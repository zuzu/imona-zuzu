#!/usr/local/bin/perl

##設定ここから##

$ver = '050412';#バージョン表記(更新日)

$d = '../dat';#datフォルダの場所

$c = '10';#処理回数の上限値

##設定ここまで##

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
print "引用符<input type=text name=inyou size=2 value=$inyou>を使用して<BR>";
print "<input type=text name=resno size=3>番の";
print "<SELECT name=seikei>";
print "<OPTION value=1>全部</OPTION>";
print "<OPTION value=2>本文</OPTION>";
print "<OPTION value=3>全部２</OPTION>";
print "<OPTION value=4>本文２</OPTION>";
print "<OPTION value=5>全部３</OPTION>";
print "<OPTION value=6>本文３</OPTION>";
print "</SELECT>";
print "<input type=submit value=COPY></form>";
print "<HR>";

$str = $dat[0];
@txt = split(/\t/,$str);
$max_resno = $txt[1];
print "<FONT size=-1>告＞最大レス番号は$max_resnoです。</FONT><HR>";

$str = $dat[1];
@txt = split(/<>/,$str);
$title = $txt[4];
print "<FORM action=i><TEXTAREA>$title$server/test/read.cgi/$directory/$datno/</TEXTAREA></FORM>";
print "<FORM action=i><TEXTAREA>編集エリア</TEXTAREA></FORM>";

###################

@resno = split(/ |　/,$resno);
$i = 0;
foreach $no (@resno){

$i++;#処理カウント
if($i > $c){
print "<FONT size=-1 color=#ff0000>注＞負荷を抑える為に$i個目で処理を中断しました。</FONT><BR>";
last;
}

if($no =~ /[^0-9]/){#数列確認
$ii++;
next;
}

if($no > $max_resno){#レス数確認
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

	if($seikei == 1){#全部
	print "<FORM action=i><TEXTAREA>";
	print "$inyou$no 名前: $txt[0] [$txt[1]] 投稿日: $txt[2]\n";
	print "$inyou$txt[3]\n";
	print "</TEXTAREA></FORM>";
	}
	elsif($seikei == 2){#本文
	print "<FORM action=i><TEXTAREA>";
	print "$inyou$txt[3]\n";
	print "</TEXTAREA></FORM>";
	}
	elsif($seikei == 3){#全部２
	print "<FORM action=i><TEXTAREA>";
	print "$inyou$no 名前: $txt[0] [$txt[1]] 投稿日: $txt[2]\n";
	print "$inyou$txt[3]\n";
	print "\n$server/test/read.cgi/$directory/$datno/$no";
	print "</TEXTAREA></FORM>";
	}
	elsif($seikei == 4){#本文２
	print "<FORM action=i><TEXTAREA>";
	print "$inyou$txt[3]\n";
	print "\n$server/test/read.cgi/$directory/$datno/$no";
	print "</TEXTAREA></FORM>";
	}
	elsif($seikei == 5){#全部３
	print "<FORM action=i><TEXTAREA>";
	print "$inyou$no 名前: $txt[0] [$txt[1]] 投稿日: $txt[2]\n";
	print "$inyou$txt[3]\n";
	print "\n$title$server/test/read.cgi/$directory/$datno/$no";
	print "</TEXTAREA></FORM>";
	}
	elsif($seikei == 6){#本文３
	print "<FORM action=i><TEXTAREA>";
	print "$inyou$txt[3]\n";
	print "\n$title$server/test/read.cgi/$directory/$datno/$no";
	print "</TEXTAREA></FORM>";
	}

}
}

if($ii > 0){
print "<FONT size=-1 color=#ff0000>注＞半角数字以外は処理しません。($ii回)</FONT><BR>";
}

if($iii > 0){
print "<FONT size=-1 color=#ff0000>注＞最大レス番号以上は処理しません。($iii回)</FONT><BR>";
}

print "<FONT size=-1>告＞処理完了しました。</FONT><HR>";
#print "<a href=help.txt>HELP</a><BR><BR>";
print "■作った人<BR>８の１６８<BR>◆iMona7m46s";
print "<BR>ご意見・ご要望は<a href=http://nov.s56.xrea.com/test/r.cgi/nov/1107780040/l10>こちら</a>へ";
exit;
