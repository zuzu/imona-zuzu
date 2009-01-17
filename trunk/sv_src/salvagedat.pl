#!/usr/bin/perl

# salvage dat files

BEGIN {	#����N�����̂�
	if(exists $ENV{MOD_PERL}){	#mod_perl�œ��삵�Ă���Ƃ�
		#�X�N���v�g�̂���f�B���N�g���̎擾
		$cdir = $ENV{SCRIPT_FILENAME};
		$cdir =~ s/\/[^\/]*$//;
	} else {
		$cdir = ".";
	}
}

($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst) = localtime();
$mon = ($mon + 1);	$year += 1900;

binmode(STDOUT);

$bypass = 0;

&getformdata;

if($FORM{'mode'} eq ''){
	print "Content-type: text/html\n\n";
	&print_html;
} elsif($FORM{'mode'} eq 'salvage') {
	print "Content-type: text/html\n\n";

	if($FORM{'data'} =~ /^bypass /){
		$bypass = 1;
		$FORM{'data'} =~ s/^bypass //;
	}

	if(&analyzeurl($FORM{'data'}) == 0){
		if(-e "$cdir/dat/$sboard/$ithread.dat"){
			print "�t�@�C����������܂����I<br>";

			if($bypass == 1){
				print "[bypasslimitmode]<br>";
				print "<a href=./salvagedat.pl?mode=dl&b=$sboard&t=$ithread&bypass=ok>�_�E�����[�h</a>"
			} elsif($hour >= 2 && $hour <= 8){
				print "����dat���_�E�����[�h���邱�Ƃ��ł��܂�<br>";
				print "�_�E�����[�h����ꍇ�͈ȉ��̃����N���E�N���b�N->�Ώۂ��t�@�C���ɕۑ����ĉ������B<br><a href=./salvagedat.pl?mode=dl&b=$sboard&t=$ithread>�_�E�����[�h</a>"
			} else {
				print "���������݂̓T�[�r�X��~���ł�<br>�T�[�r�X���ԑтɍēx�����ĉ�����";
			}
			
		} else {
			print "�G���[<br>�c�O�Ȃ���t�@�C�������݂��܂���";
		}
	} else {
		print "�G���[<br>url����͂ł��܂���";
	}
} elsif($FORM{'mode'} eq 'dl') {
	$sboard = $FORM{'b'};
	$ithread = $FORM{'t'};

	if($FORM{'bypass'} eq 'ok'){
		$bypass = 1;
	}
	
	if(-e "$cdir/dat/$sboard/$ithread.dat"){
		if(($hour >= 2 && $hour <= 8) || $bypass == 1){
			&downloaddat;
		} else {
			print "Content-type: text/html\n\n";
			print "���݂̓T�[�r�X��~���ł�<br>�T�[�r�X���ԑтɍēx�����ĉ�����";
		}
	} else {
		print "Content-type: text/html\n\n";
		print "�G���[<br>�c�O�Ȃ���t�@�C�������݂��܂���";
	}
}


exit();


sub print_html {
print <<"_HTML_";
<html>
imona.net���ɃL���b�V���Ƃ��ĕۑ�����Ă���dat���_�E�����[�h�ł��܂��B<br>
����̕ی�̂��߁A�g�p�ł��鎞�Ԃ͌ߑO2���`�ߑO8���݂̂ł��B<br>

<form action=salvagedat.pl method=post>
<input type=hidden name="mode" value="salvage">
dat���_�E�����[�h�������X���b�h��URL���w�肵�ĉ������B<br>
<input name=data type=text value="http://" size=100><br>
<input type=submit value="dat�̃_�E�����[�h">
</form>
</html>
_HTML_
}

sub downloaddat {
	
open(FILE , "$cdir/dat/$sboard/$ithread.dat");
	binmode(FILE);
	<FILE>;

print <<"_HEADER_";
Content-Type: application/octet-stream; name=$ithread.dat
Content-Disposition: attachment; filename=$ithread.dat

_HEADER_

	while(<FILE>){
		print;
	}
close(FILE);

}

sub analyzeurl {
	my $userver = $_[0], $upath;
	
	if($userver =~ /^(h?ttp\:\/\/[^\/]+)(\/.+)$/){
		$userver = $1;
		$upath = $2;
		if($userver =~ /^h?ttp\:\/\/[0-9]+$/ || $userver =~ /[\.\/]2ch\.net/ || $userver =~ /[\.\/]bbspink\.com/){	#2ch��URL
		
			$upath =~ s/\.html//g;
			$upath =~ s|/i/|/|g;
			$upath =~ s|/read\.cgi/|/|g;
			$upath =~ s|/p\.i/|/|g;
			$upath =~ s|/r\.i/|/|g;
			$upath =~ s|/test/|/|g;
			if($upath =~ /([0-9]{9,10})/){	#���X�̕\��
				$ithread = $1;
				$upath =~ s|/([0-9]{9,10})/|/|;
			}
			$upath =~ s|//+|/|g;

			if($upath =~ /\/([A-Za-z0-9]{2,12})\// ){	#�����擾
				$sboard = $1;
				#$upath =~ s|/([A-Za-z0-9]{2,12})/|/|;
			} else {	#�����擾�ł��Ȃ�
				return -1;
			}

			return 0;
		} else {	#2ch�OURL�̓G���[
			return -1;
		}
	} else {	#URL�łȂ����̂̓G���[
		return -1;
	}
}

sub getformdata {
	if ($ENV{'REQUEST_METHOD'} eq "POST") {
		read(STDIN, $buffer, $ENV{'CONTENT_LENGTH'});
		}
	else {
		$buffer = $ENV{'QUERY_STRING'};
	}

	@buffer = split(/&/, $buffer);

	foreach $pair (@buffer) {
		local($name, $value) = split(/=/, $pair);
		$name	=~ tr/+/ /;
		$name	=~ s/%([a-fA-F0-9][a-fA-F0-9])/pack("C", hex($1))/eg;
		$value	=~ tr/+/ /;
		$value	=~ s/%([a-fA-F0-9][a-fA-F0-9])/pack("C", hex($1))/eg;
		$value	=~ s/</&lt;/ig;
		$value	=~ s/>/&gt;/ig;
		$value	=~ s/\r\n/<br>/g;
		$value	=~ s/\n/<br>/g;
		$value	=~ s/\r/<br>/g;
		$value	=~ s/\,/&#44;/g;
		$FORM{$name} = $value;
	}
}
