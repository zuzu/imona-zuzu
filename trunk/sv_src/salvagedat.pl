#!/usr/bin/perl

# salvage dat files

BEGIN {	#初回起動時のみ
	if(exists $ENV{MOD_PERL}){	#mod_perlで動作しているとき
		#スクリプトのあるディレクトリの取得
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
			print "ファイルが見つかりました！<br>";

			if($bypass == 1){
				print "[bypasslimitmode]<br>";
				print "<a href=./salvagedat.pl?mode=dl&b=$sboard&t=$ithread&bypass=ok>ダウンロード</a>"
			} elsif($hour >= 2 && $hour <= 8){
				print "現在datをダウンロードすることができます<br>";
				print "ダウンロードする場合は以下のリンクを右クリック->対象をファイルに保存して下さい。<br><a href=./salvagedat.pl?mode=dl&b=$sboard&t=$ithread>ダウンロード</a>"
			} else {
				print "しかし現在はサービス停止中です<br>サービス時間帯に再度試して下さい";
			}
			
		} else {
			print "エラー<br>残念ながらファイルが存在しません";
		}
	} else {
		print "エラー<br>urlが解析できません";
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
			print "現在はサービス停止中です<br>サービス時間帯に再度試して下さい";
		}
	} else {
		print "Content-type: text/html\n\n";
		print "エラー<br>残念ながらファイルが存在しません";
	}
}


exit();


sub print_html {
print <<"_HTML_";
<html>
imona.net内にキャッシュとして保存されているdatをダウンロードできます。<br>
回線の保護のため、使用できる時間は午前2時～午前8時のみです。<br>

<form action=salvagedat.pl method=post>
<input type=hidden name="mode" value="salvage">
datをダウンロードしたいスレッドのURLを指定して下さい。<br>
<input name=data type=text value="http://" size=100><br>
<input type=submit value="datのダウンロード">
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
		if($userver =~ /^h?ttp\:\/\/[0-9]+$/ || $userver =~ /[\.\/]2ch\.net/ || $userver =~ /[\.\/]bbspink\.com/){	#2chのURL
		
			$upath =~ s/\.html//g;
			$upath =~ s|/i/|/|g;
			$upath =~ s|/read\.cgi/|/|g;
			$upath =~ s|/p\.i/|/|g;
			$upath =~ s|/r\.i/|/|g;
			$upath =~ s|/test/|/|g;
			if($upath =~ /([0-9]{9,10})/){	#レスの表示
				$ithread = $1;
				$upath =~ s|/([0-9]{9,10})/|/|;
			}
			$upath =~ s|//+|/|g;

			if($upath =~ /\/([A-Za-z0-9]{2,12})\// ){	#板名を取得
				$sboard = $1;
				#$upath =~ s|/([A-Za-z0-9]{2,12})/|/|;
			} else {	#板名が取得できない
				return -1;
			}

			return 0;
		} else {	#2ch外URLはエラー
			return -1;
		}
	} else {	#URLでないものはエラー
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
