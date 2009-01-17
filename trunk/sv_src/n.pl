#!/usr/local/bin/perl

# N速+ Headline news

##/設定部分/########################

$ENV{'TZ'} = "JST-9";
$dat = './dat';
$brd3 = 'brd5.txt';

####################################

BEGIN {	# 初回起動時のみ
	if(exists $ENV{MOD_PERL}){	# mod_perlで動作しているとき
		# カレントディレクトリを変更する
		$cdir = $ENV{SCRIPT_FILENAME};
		$cdir =~ s/\/[^\/]*$//;
		chdir $cdir;
	}
}


require "jcode.pl";
require "admgr.pl";

print "Content-type: text/html\n\n";

&getformdata;

$now = time;
($sec, $min, $hour, $mday, $mon, $year, $wday, $yday, $isdst) = localtime($now);
$offset = $now - (((($mday * 24 + $hour) * 60) + $min) * 60 + $sec) + 32 * 60 * 60 * 24;

if($board == -1){$brddir = "$dat/newsplus";}
else {$brddir = "$dat/" . (split( /\//, &boradurl(to10($board))))[3];}

if($FORM{'r'} eq ''){
	$now -= $FORM{'d'} * 60 * 60 * 12;
	($sec, $min, $hour, $mday, $mon, $year, $wday, $yday, $isdst) = localtime($now);
	$today = $now - ((($hour * 60) + $min) * 60 + $sec);
	if($hour >= 12){$today += 60 * 60 * 12;}
	$after12h = $today + 60 * 60 * 12;

	&printtop;
} else {
	$dattime = $offset - &to10($FORM{'r'});
	open(READ , "$brddir/$dattime.dat");
		<READ>;	$data = <READ>;
	close(READ);
	chomp($data);	@res = split(/<>/, $data);

	$res[3] =~ s%(https?)\:([\w|\:\!\#\$\%\=\&\-\^\`\\\|\@\~\[\{\]\}\;\+\*\,\.\?\/]+)%linksub($1, $2)%ieg;
	$res[3] =~ s/ ?<br> ?/<br>/g;
	$res[2] =~ s/ ID:\?\?\?//;
	$res[0] =~ s/ ?<\/?b> ?//g;
	$out = "$res[0] $res[2]<br>$res[3]";

	print "<center>";	&ad_img;	print "</center>";
}

&jcode'convert(*out, 'euc' , 'sjis'); # eucに変換
&jcode'z2h_euc(*out);        # 全角カナを半角カナに変換
&jcode'tr(*out, '０-９Ａ-Ｚａ-ｚ', '0-9A-Za-z');	# 全角英数字を半角英数字に変換する
&jcode'tr(*out, '”＃＄％＆’｜￥＾！　（）｛｝［］：；＋＊＝＜＞？／＿＠−', '"#$%&\'|\\^! (){}[]:;+*=<>?/_@-');	# 全角スペースなどを半角スペースなどに変換する
&jcode'convert(*out, 'sjis' , 'euc'); # sjisに変換

print $out;

print "<br><br>";	&ad;

exit;

sub printtop{
	if($FORM{'d'} == 0){
		if($board == -1){
			print "<title>N\x91\xAC+ Headline news</title>";	#N速+ Headline news
		} else {
			print "<title>Headline news</title>";	#N速+ Headline news
		}
	}
	print "$mday\x93\xFA ";
	if($hour >= 12){
		if($board == -1){
			print "\x8C\xDF\x8C\xE3 <a href=n.pl?\@" . ($FORM{'d'}+1) . ">\x8C\xDF\x91\x4F</a><hr>";	#午後 午前
		} else {
			print "\x8C\xDF\x8C\xE3 <a href=n.pl?$board!\@" . ($FORM{'d'}+1) . ">\x8C\xDF\x91\x4F</a><hr>";	#午後 午前
		}
	} else {
		if($board == -1){
			print "\x8C\xDF\x91\x4F <a href=n.pl?\@" . ($FORM{'d'}+1) . ">\x91\x4F\x93\xFA</a><hr>";
		} else {
			print "\x8C\xDF\x91\x4F <a href=n.pl?$board!\@" . ($FORM{'d'}+1) . ">\x91\x4F\x93\xFA</a><hr>";
		}
	}

	if(&ad() != 1){
		print "<hr>";
	}

	opendir( FILES, "$brddir" );
	@files = readdir( FILES );
	close(FILES);

	@files = sort { $b <=> $a } @files;
	$view = 0;	$out = '';

	foreach $file ( @files ){
		$dattime = $file;
		$dattime =~ s/\.dat//;

		if($after12h > $dattime && $dattime >= $today){
			$view = 1;
			($dsec, $dmin, $dhour, $dmday, $dmon, $dyear, $dwday, $dyday, $disdst) = localtime($dattime);
			open(READ , "$brddir/$file");
				$title = <READ>;	$title = <READ>;
			close(READ);
			chomp($title);	$title =~ s/^.+<>//;
			if($board == -1){
				$out .= sprintf("%02d:%02d", $dhour, $dmin) . " <a href=n.pl?" . to79($offset - $dattime) . ">$title</a><br>";
			} else {
				$out .= sprintf("%02d:%02d", $dhour, $dmin) . " <a href=n.pl?$board!" . to79($offset - $dattime) . ">$title</a><br>";
			}
		} elsif($view == 1) {
			last;
		}
	}

}

sub linksub {
	local($prot, $link) = @_;
	$link =~ s/(\W)/sprintf("@%02X", ord($1))/ego;
	return "<a href=http://wmlproxy.google.com/chtmltrans.bin/h=ja/p=i/u=$prot:$link/c=0>URL</a>"
}

sub to10{
	my ($i, $j, $ret);
	$i = $_[0];	$ret = 0;
	while(($i =~ s/^(.)//) == 1){
		$j = unpack("C", $1);
		if($j >= 96){$j -= 1}
		if($j >= 92){$j -= 1}
		if($j >= 58){$j -= 7}
		if($j >= 47){$j -= 1}
		if($j >= 37){$j -= 1}
		$j -= 36;
		$ret = $ret * 79 + $j;
	}
	return $ret;
}

sub to79{
	my ($i, $j, $ret);
	$i = $_[0], $ret = '';
	
	while($i > 0){
		$j = $i % 79 + 36;
		if($j >= 37){$j += 1}
		if($j >= 47){$j += 1}
		if($j >= 58){$j += 7}
		if($j >= 92){$j += 1}
		if($j >= 96){$j += 1}
		$ret = chr($j) . $ret;
		$i = int($i / 79);
	}
	return $ret;
}

sub getformdata {
	$buffer = '';
	if ($ENV{'REQUEST_METHOD'} eq "POST") {
		read(STDIN, $buffer, $ENV{'CONTENT_LENGTH'});
	} else {
		$buffer = $ENV{'QUERY_STRING'};
	}

	#@buffer = split(/&/, $buffer);
	%FORM = ();	$board = -1;
	if($buffer =~ /^([^\!]+)\!(.*)$/){
		$board = $1;	$buffer = $2;
	}
	
	if($buffer =~ /^\@(.+)$/){
		$FORM{'d'} = $1
	} else {
		$FORM{'r'} = $buffer;
	}

	#foreach $pair (@buffer) {
	#	local($name, $value) = split(/=/, $pair);
		#$name	=~ tr/+/ /;
	#	$name	=~ s/%([a-fA-F0-9][a-fA-F0-9])/pack("C", hex($1))/eg;
		#$value	=~ tr/+/ /;
	#	$value	=~ s/%([a-fA-F0-9][a-fA-F0-9])/pack("C", hex($1))/eg;
	#	$FORM{$name} = $value;
	#}
}

sub boradurl {	#板の番号からURLを取得
	$i = 0;
	@line = ();

	if($_[0] == 9000){
		return 'dempa2ch';
	}

	if(open(DATA, "$brd3")){
		binmode(DATA);
		@line = <DATA>;
		close(DATA);

		$i = int($_[0] / 100);
		$line[$i] =~ s/^([^\r\n]*)[\r\n]*$/$1/;
		if($line[$i] eq ""){
			return '';
		}

		if($line[$i] =~ /\t/){
			@array = split( /\t/, $line[$i]);
			return $array[$_[0] - $i * 100];
		} else {
			return $line[$i];
		}
	}
	return '';
}


sub ad_img{
	#携帯でなければ広告は出力しない
	if(! &pAdManager'isMobile()){ return 1; }

	&pAdManager'putAd("ad_image");

	return 0;
}

sub ad{
	#携帯でなければ広告は出力しない
	if(! &pAdManager'isMobile()){ return 1; }

	&pAdManager'putAd("ad_text");

	return 0;
}

