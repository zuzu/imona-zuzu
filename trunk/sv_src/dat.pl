#!/usr/local/bin/perl

# 2ch�u���E�U�e�X�g

#������͉��H
# html�u���E�U�Ŏg�p����2ch�u���E�U�ł��B
# DAT���ǂ݂���̂ŃL���b�V���������܂��B
# �������݂͂��̃X�N���v�g�͒ʂ����A�ʏ�Ɠ���URL��@���܂��B

#���g�p���@
# �ȉ��̐ݒ�𖄂߂���ŁA
# http://�`/dat.pl

#���d�l
# http://�`/dat.pl/r/AAA.2ch.net/BOARDNAME/
# http://�`/dat.pl/r/AAA.2ch.net/test/read.cgi/BOARDNAME/1234567890/
# http://�`/dat.pl/r/AAA.2ch.net/test/read.cgi/BOARDNAME/1234567890/l50
# http://�`/dat.pl/r/AAA.2ch.net/BOARDNAME/1234567890/123-456
# �ȂǓK���ɁB

#���ݒ�
do 'setting.pl';
$url = 'http://imona.net/';	# URL�̎w��

#�ȉ��͕ύX�s�v
$nres = 9999;	#�ő僌�X�ǂݍ��ݐ�����
$script = 'dat.pl';	#�X�N���v�g��
#$bbsmenu = 'bbsmenu.html';	#bbsmenu

#&getformdata;
$buffer = $ENV{PATH_INFO};

print "Content-type: text/html\n\n";


if($buffer =~ /^\/?r\/(.*)$/){	#read.cgi
	&read();
} elsif($buffer =~ /^\/?menu\/?$/) {	#�ꗗ�̕\��
	require 'http.pl';
	$http'ua = 'Monazilla/1.00 (iMona/1.0)';	#USER-AGENT
	$http'range = 0;
	$http'other = '';
	
	$data = &http'get('http://www.ff.iij4u.or.jp/~ch2/bbsmenu.html');	#�ꗗ�̎擾
	$data =~ s%<A HREF=http://([^\.]+\.(2ch\.net|bbspink\.com)/[^\.]+/)>%<A HREF=$url$script/r/$1>%ig;
	print $data;
} elsif($buffer =~ /^\/?title\/?$/) {	#�^�C�g���̕\��
print <<"_HTML_";
<html>
2ch�u���E�U�e�X�g
</html>
_HTML_
} else {	#index
print <<"_HTML_";
<html><head><title>�@��2ch BBS ..</title>
<meta http-equiv="Content-Type" content="text/html; charset=x-sjis">
</head>
<frameset cols="103,*">
<frame src="./$script/menu" name="menu" MARGINWIDTH=0 MARGINHEIGHT=4 FRAMEBORDER=0>
<frame src="./$script/title" name="cont" MARGINWIDTH=0 MARGINHEIGHT=0 FRAMEBORDER=0>
</frameset>
</html>
_HTML_
}

print "<br><br>dat.pl ver1.00<br><br>\n";
($user,$system,$cuser,$csystem) = times;
print "�g�pCPU����<br>[user:$user,system:$system,���v:" . ($user + $system) ."]<br>";
print "pathinfo: $ENV{PATH_INFO}";
exit();

sub read {
	$/ = "\x0A";	#���s�R�[�h��\x0A(LF)�ɂ���B
	binmode(STDOUT);

	$_path = $1;
	$path = $1;

	$path =~ s/^([^\/]*)\//\//;
	$server = $1;

	if($path =~ /\.html/){	#HTML���ς݂̃t�@�C��,subback.html�̏ꍇ
		require 'http.pl';
		$http'ua = 'Monazilla/1.00 (iMona/1.0)';	#USER-AGENT
		$http'range = 0;
		$http'other = '';

		print "[http://$_path]";
		$data = &http'get("http://$_path");
		$data =~ s%<base href="http://([^\" ]+)%<base href="$url$script/r/$1%i;
		&link();
		print $data;
		return;
	}

	#����Ȃ������폜
	$path =~ s|/i/|/|g;
	$path =~ s|/read.cgi/|/|g;
	$path =~ s|/p.i/|/|g;
	$path =~ s|/r.i/|/|g;
	$path =~ s|/test/|/|g;

	if($path =~ /([0-9]{9,10})/){	#���X�̕\��
		$ithread = $1;
		$path =~ s|/([0-9]{9,10})/|/|;
	}
	$path =~ s|//+|/|g;

	if($path =~ /\/([A-Za-z0-9]{2,10})\// ){	#�����擾
		$board = $1;
	} else {	#�����擾�ł��Ȃ�
		print "�G���[�ł�<br>�̖��O���擾�ł��܂���ł���<br>�������悤�Ƃ����f�[�^�F$_path<br>���݂̃f�[�^�F$path";
		return;
	}

	if($ithread != 0){	#���X�̕\��
		if($path =~ /\/(l?)([0-9]+)(\-?)([0-9]*)$/){	#���X�ʒu�w�肪����ꍇ
			if($1 ne ''){#last
				$last = $2;
				if($last > $nres){
					$last = $nres;
				}
				$op = 'f';
			} else {
				if($3 ne ''){#-
					$start = $2;
					if($4 eq '' || $4 - $2 > $nres){
						$to = $start + $nres;	$op = 'f';
					} else {
						$to = $4;
					}
				} else {	#1���X���w��
					$start = $2;
					$to = $2;
				}
			}
		} else {	#�����ꍇ
			$start = 1;	$to = $nres; $op = 'f';
		}

		require '2c.pl';
		$data = &p2chcache'read($server, $board, $ithread, $start, $to, $last, $op);
		&link();
		@data = split(/\n/, $data);
		$data[0] =~ m/Res:([0-9]*)\-([0-9]*)\/([0-9]*)/i;	#�n��-�I���/�S���̐�
		$st = $1;	$end = $2; $all = $3;
		printres();
		#print @data;
	} else {	#�̕\��
		require 'http.pl';
		$http'ua = 'Monazilla/1.00 (iMona/1.0)';	#USER-AGENT
		$http'range = 0;
		$http'other = '';

		$data = &http'get("http://$_path");

		&link();
		print $data;
	}
}


sub printres {
print '<html><head><meta http-equiv="Content-Type" content="text/html; charset=Shift_JIS"><title>';

$title = (split(/<>/, $data[1]))[4];
print $title;

print <<"_HTML_";
</title><script type="text/javascript" defer><!--
function l(e){var N=g("NAME"),M=g("MAIL"),i;with(document) for(i=0;i<forms.length;i++)if(forms[i].FROM&&forms[i].mail)with(forms[i]){FROM.value=N;mail.value=M;}}onload=l;function g(key,tmp1,tmp2,xx1,xx2,xx3,len){tmp1=" "+document.cookie+";";xx1=xx2=0;len=tmp1.length;while(xx1<len){xx2=tmp1.indexOf(";",xx1);tmp2=tmp1.substring(xx1+1,xx2);xx3=tmp2.indexOf("=");if(tmp2.substring(0,xx3)==key)return unescape(tmp2.substring(xx3+1,xx2-xx1-1));xx1=xx2+1;}return "";}
//--></script></head><body bgcolor=#efefef text=black link=blue alink=red vlink=#660099><a href="../../../../$board/">���f���ɖ߂遡</a> 
_HTML_

print "<a href=./>�S��</a> ";

for($i = 0; $i <= ($all/100); $i++){
	$j = $i * 100+1;	$k = ($i+1) * 100;
	if($i == 0){
		print "<a href=-100>1-</a> ";
	} else {
		print "<a href=$j-$k>$j-</a> ";
	}
}

print <<"_HTML_";
<a href=l50>�ŐV50</a><p><font size=+1 color=red>$title </font><dl>
_HTML_

if($end > $all){$end = $all;}

if($st == 0){
	print "�\\�����郌�X������܂���<br><br>(dat�����A���X�w��͈̓G���[�A�o�O�Ȃ�)";
	$end = $start - 1;
} else {
#	for($i = 0; $i <= ($end - $st); $i++){
	for($i = 0; $i <= ($end - $st) + 1; $i++){
		if($op ne ''){
			if($i == 0){
				$resnum = 1;
			} elsif($st == 1) {
				$resnum = $st+$i;
			} else {
				$resnum = $st+$i-1;
			}
		} else {
			$resnum = $st+$i;
		}

		if($data[$i+1] eq ''){next;}
		
		@res = split(/<>/, $data[$i+1]);
		if($res[1] ne ''){	#���[����
			print "<dt>" . $resnum . " �F<a href=\"mailto:$res[1]\"><b>$res[0]</b></a> �F$res[2]<dd>$res[3]<br><br>";
		} else {
			print "<dt>" . $resnum . " �F<font color=green><b>$res[0]</b></font> �F$res[2]<dd>$res[3]<br><br>";
		}
	}
}

print <<"_HTML_";
</dl><font color=red face="Arial"><b>XXX KB</b></font>&nbsp;&nbsp;<font size=2><b>[ �Q�����˂���g���Ă���<font size=4 color=tomato>���S�ш�ۏ�</font>��p�T�[�o <a href="http://www.maido3.com/server/line-up/" target="_blank">Big-Server.com</a> ]</b> 30,000�~/��</font>
_HTML_

if($all == $end || $st == 0){
	print "<hr><center><a href=" . $end . "->�V�����X�̕\\��</a></center><hr>";
} else {
	print "<hr><center><a href=" . ($end+1) . "->������ǂ�</a></center><hr>";
}

if($end < $all){
	print "<a href=" . ($end+1) . "-" . ($end+100) . ">��100</a>";
}

print <<"_HTML_";
 <a href=l50>�ŐV50</a> <s>(08:00PM - 02:00AM �̊Ԉ�C�ɑS���͓ǂ߂܂���)</s><br>
<form method=POST action="http://$server/test/bbs.cgi" target="_blank"><input type=submit value="��������" name=submit> ���O�F <input name=FROM size=19> E-mail<font size=1> (�ȗ���) </font>: <input name=mail size=19><br><textarea rows=5 cols=70 wrap=off name=MESSAGE></textarea><input type=hidden name=bbs value=$board><input type=hidden name=key value=$ithread><input type=hidden name=time value=$ithread></form><p>read.cgi verX.XX (XX/XX/XX)</body></html>
_HTML_

}

sub link {
	$data =~ s%([^\"=h])h?ttp(s?)://([-_.!~*'()a-zA-Z0-9;\/?:\@&=+\$,\%#]+)%$1<a href="http$2://ime.nu/$3" target="_blank">http$2://$3</a>%g;

	$data =~ s%<A HREF=(\"?)\.\./%<A HREF=$1http://$server/%ig;
	$data =~ s%<A HREF=(\"?)\./%<A HREF=$1http://$_path%ig;
	$data =~ s%<A HREF=(\"?)http://([^\.]+\.(2ch\.net|bbspink\.com)/[^\" ]+)%&urlcheck($1,$2)%ieg;
	$data =~ s%<A HREF=(\"?)http://ime.nu/([^\.]+\.(2ch\.net|bbspink\.com)/[^\" ]+)%&urlcheck($1,$2)%ieg;

	$data =~ s%<form method=POST action=\"[\.\/]+(test)?/bbs\.cgi\">%<form method=POST action="http://$server/test/bbs.cgi" target="_blank">%ig;
}

sub urlcheck {
	if($_[1] =~ /^www/){
		return "<A HREF=$_[0]http://";
	} else {
		return "<A HREF=$_[0]$url$script/r/$_[1]";
	}
}

sub getformdata {
	if ($ENV{'REQUEST_METHOD'} eq "POST") {
		read(STDIN, $buffer, $ENV{'CONTENT_LENGTH'});
	} else {
		$buffer = $ENV{'QUERY_STRING'};
	}
}
