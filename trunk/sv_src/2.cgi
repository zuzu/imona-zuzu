#!/usr/local/bin/perl
###################################################################################################

#
# 2.cgi - ZuzuOriginal
#		// 2ch viewer for i��ppli,EZappli,Vappli
#		// [charset:EUC-JP]
#		// ���Υ�����ץȤ�EUC-JP������Ȥ��Ƥ��ޤ���¾��ʸ�������ɤ��Ѵ�����������ư��ޤ���
#

###################################################################################################


BEGIN {	# ���ư���Τ�
	#use FCGI;

	if(exists $ENV{MOD_PERL}){	# mod_perl��ư��Ƥ���Ȥ�
		# �����ȥǥ��쥯�ȥ���ѹ�����
		$cdir = $ENV{SCRIPT_FILENAME};
		$cdir =~ s/\/[^\/]*$//;
		chdir $cdir;

		##/libraries/########################
		require '2c.pl';
		require Compress::Zlib;
		####################################
	}
	
	#if($fastcgi == 1){
		##/libraries/########################
	#	require '2c.pl';
	#	require Compress::Zlib;
		####################################

	#}

	##/������ʬ/########################
	do 'setting.pl';
	####################################
	
	##/libraries/########################
	require "jcode.pl";
	if($encodemod >= 1){	# Encode �⥸�塼�����Ѥ���
		require Encode;
		require Encode::JP::H2Z;
		$sjis_enc = Encode::find_encoding("Shift-JIS");
		$euc_enc = Encode::find_encoding("euc-jp");

	}
	if($encodemod >= 2){	# Drk::Encode �⥸�塼�����Ѥ���
		require Drk::Encode;
		$DrkEncode = Drk::Encode->new( ascii => 1 );
	}
	####################################
}

## ���� ###########################################################################################
$brd = 'brd.txt';				# �ĥǡ���(board.cgi�Ǻ����������)
$brd2 = 'brd2.txt';				# �ĥǡ���(board.cgi�Ǻ����������) ü���˽��Ϥ���
$brd3 = 'brd3.txt';				# �ĥǡ���(board.cgi�Ǻ����������) ���ֹ�ݡ�URL�Ѵ���
$brd4 = 'brd4.txt';				# �������ĥǡ���(board.cgi�Ǻ����������) ü���˽��Ϥ���
$brd5 = 'brd5.txt';				# �������ĥǡ���(board.cgi�Ǻ����������) ���ֹ�ݡ�URL�Ѵ���
$flexiblebrd = 'brdflex.txt';	# ver15�ʾ�ǻ��Ѥ����İ����ǡ��� ü���˽��Ϥ���

$plugindir = './plugins';		# �ץ饰����Υǥ��쥯�ȥ�
$plugin = 'plugins.txt';		# �ץ饰����ꥹ��

$compw = 'compw.txt';			# û�̤��륭�����

$hostshitaraba = 'jbbs.livedoor.jp';	# ������ФΥۥ���
$hostmatibbs = 'machi.to';	# �ޤ�BBS�Υۥ���

$imonathread = 1171697443;				# iMona����åɤΥ����ֹ�
###################################################################################################
$ver2cgi = '1.65';	# ���Υ�����ץȤΥС������
###################################################################################################


$output = '';

binmode(STDOUT);

# �Ť�DAT�ξõ�
if($deldat == 1 && $windows == 0){# windows�Ķ��Ǥʤ����
	$hour = (localtime(time))[2];
	if(time > (stat("$dat/$deldatchk"))[9] + 60*60*24 && $deldatst <= $hour && $hour <= $deldatend){	# 1���ʾ�õ��Ƥ��ʤ����
		open(WRITE, ">> $dat/$deldatchk");
		if(flock(WRITE, 6) != 0){	# ��å�����Ƥ��ʤ���(��Ť�dat�ξõ�ޥ�ɤ�����Τ��򤱤�)
			print WRITE localtime(time) . "\n";
			system("find $dat/ -type f -atime +$deldatdays -exec rm {} \\;");
		}
		close(WRITE);
	}
}

&getformdata();	# �ե�����ǡ��������

$imode = 0;	$kddi = 0;	$jphone = 0;	$other = 0;
$user_agent = $ENV{'HTTP_USER_AGENT'};
if (index($user_agent,'DoCoMo') == 0) {#$user_agent[0] eq 'DoCoMo') {
	$imode = 1;
} elsif (index($user_agent,'KDDI') == 0) {
	# EZweb WAP2.0 �б�ü���Ѥν���
	$kddi = 1;
} elsif (index($user_agent,'J-PHONE') == 0 || index($user_agent,'Vodafone') == 0 || index($user_agent,'SoftBank') == 0) {
	# J-SKY �Ѥν���
	$jphone = 1;
} else {
	$other = 1;
}

$mode			= $FORM{'m'};	# �⡼�� b t m s u U w src me ver
$option			= $FORM{'p'};	# ���ץ���� x:���̤ʤ� i:IDȴ�� t:����ȴ�� m:�᡼��ȴ�� n:̾��ȴ�� p:������ˡ a:AAȴ�� u:URLȴ��
$iboard			= $FORM{'b'};	# ���ֹ�
$ithread		= $FORM{'t'};	# ����å��ֹ�
$range			= $FORM{'c'};	# �ϰ� sXXXtYYYlZZZ start,to,start��to,last�����Ѳ�ǽ ����ʤ��ξ�硢s1t5
$ver			= $FORM{'v'};	# �С������
$sword			= $FORM{'w'};	# õ��ñ��
$url			= $FORM{'u'};	# URL

#debug
#$iboard = 8101;
#$FORM{'o'} = "http://test.com/test/";
#$ver="F";
#$option="p3";
#$ithread=10060001;
#$range="s1t21";

if($mode eq 'ver' || $mode eq 'modperl'){	# version mod_perl
	&print_ua(1);
	print "2.cgi version:$ver2cgi\nsetting:$rawmode,$packmode\n";

	if(exists $ENV{MOD_PERL}){	# mod_perl��ư��Ƥ���
		print "running under mod_perl($ENV{MOD_PERL})\n";
	} else {
		print "not running under mod_perl\n";
	}

	print "using jcode.pl ver: $jcode::version\n";
	if($encodemod >= 1){	# Encode �⥸�塼�����Ѥ���
		print "using Encode::JP module ver: $Encode::JP::VERSION\n";
	}
	if($encodemod >= 2){	# Drk::Encode �⥸�塼�����Ѥ���
		print "using Drk::Encode module ver: $Drk::Encode::VERSION\n";
	}

	#exit;
}



if($log == 1){
	&log;
}

# ������������ʤ��褦�ˡ�������ǽ������ɽ�����Ѵ����롣
#$sword =~ s/([\x81-\x9f\xe0-\xfc])(.)/&sjis2reg($1,$2)/eg;

if(unpack('C',$ver) >= 65){
	$ver = unpack('C',$ver) - 55;
} else {
	$ver = 0 + $ver;
}

if($ver >= 11){
	$brd2 = $brd4;
	$brd3 = $brd5;
}

if($mode eq 'src'){	# �������ɤ�
	
	open(DATA, '2.cgi');
	binmode(DATA);
	while(<DATA>){
		$output .= $_;
	}
	close(DATA);

	&output;
	
	exit;
}

$start = 0;	$to = 0;	$last = 0;
$nita = 0;	$nres = 0;
$delid = 0;	$deltime = 0;	$delmail = 0;	$delname = 0;
$delaa = 0;	$delurl = 0;	$delret = 0;
$compression = '';	$compressmode = 0;
$deflate = 0;	$gzip = 0;	$zip = 0;	$nogzipheader = 0;	$encgzip = 0;	$sendpackedsize = 0;	$forcecompress = 0;
$reduceerror = 0;

if($range =~ /t([0-9]*)/){$to = $1;}
if($range =~ /l([0-9]*)/){$last = $1;}
if($range =~ /b([0-9]*)/){$nita = $1;}
if($range =~ /r([0-9]*)/){$nres = $1;}

if($option =~ m/c([0-9]*)/){$cushionpage = $1;}
if($option =~ m/x/){$compression = 'x';	$compressmode = 3;}
if($option =~ m/i/){$delid = 1;}
if($option =~ m/t/){$deltime = 1;}
if($option =~ m/m/){$delmail = 1;}
if($option =~ m/n/){$delname = 1;}
if($option =~ m/p([0-9])/){$compressmode = $1;}
if($option =~ m/a([0-9])/){$delaa = $1;}
elsif($option =~ m/a/){$delaa = 2;}
if($option =~ m/u([0-9]*)/){$delurl = $1 - 1;	if($1 eq ''){$delurl = 1;}}
if($option =~ m/r([0-9]+)/){$delret = $1;}
if($option =~ m/d([0-9]*)/){
	$deflate = 1;
	$gzip = $1;
	if($gzip eq ""){$gzip = 6;}
}
if($option =~ m/g([0-9]*)/){
	if($jphone == 1){
		$deflate = 1;
	}
	$gzip = $1;
	if($gzip eq ""){$gzip = 6;}
}
if($option =~ m/z([0-9]*)/){
	$zip = 1;
	$gzip = $1;
	if($gzip eq ""){$gzip = 6;}
}
if($option =~ m/h/){$nogzipheader = 1;}
if($gzip == 0 && $ENV{'HTTP_ACCEPT_ENCODING'} =~ /gzip/ && $packmode != 0){
	$gzip = 6;
	$encgzip = 1;
}
if($option =~ m/s/){$sendpackedsize = 1;}
if($option =~ m/f/){$forcecompress = 1;}
if($option =~ m/r[^0-9].+$/){$reduceerror = 1;}

if($range =~ /s([0-9]*)/){$start = $1;}

#���å�����󥯥��ץ����
if($option =~ m/\/([0-9]+)([A-Z]+)/){$sp = $1;	$ext = $2; $cushionpage = 1;}#else{
#	if($option =~ m/c([0-9]+)([A-Z]+)/){$sp = $1;	$ext = $2;}
#}


if($compressmode == 3){$compression = 'x';}

if($mode eq 's' && $sword eq ''){	# sword��ά��
	$mode = 't';
}

if($mode eq ''){	# mode��ά��
	if($iboard eq '' && $ithread eq ''){
		$mode = 'b';
	} elsif($iboard ne '' && $ithread eq ''){
		$mode = 't';
	} elsif($iboard ne '' && $ithread ne ''){
		$mode = 'm';
	}
}

# thread�ֹ�˥��ߤ��Ĥ��Ƥ���Ȥ��Ͻ���
$ithread =~ s/[^0-9]+$//;

if(open(DATA, "$compw")){	# ���̤��륭����ɤ��ɤ߽Ф�
	binmode(DATA);
	@compw = <DATA>;
	chomp(@compw);
	close(DATA);
}

if($mode eq 'u'){	# URL��ɽ��
## ���� ##
# $mode eq 'u'	# URL��ɽ��
# ���顼���֤��Ȥ�
# \x00 ['0'|'A'|'B'] 0:����Ū�ʥ��顼 A:���쥹���ʤ� B:2ch�ؤ���³���顼
# �쥹���֤��Ȥ�
# \x01 &to240($iboard) \t &to240($ithread) \n
# \x03 &to240($iboard) \t &to240($ithread) \t $stname \n(�����ȥ��դ�,$ver==12)
# \x13 &to240($iboard) \t &to240($ithread) \t $stname \n(�����ȥ��դ�,$ver>=13)
# �İ������֤��Ȥ�
# \x02 &to240($iboard) \n
# \x04 &to240($iboard) \t $sbname \n(�����ȥ��դ�,$ver==12)
# \x14 &to240($iboard) \t $sbname \n(�����ȥ��դ�,$ver>=13)
#
# $iboard:�ɤ߹�����Ĥ����ֹ�
# $ithread:�ɤ߹��������åɤ��ֹ�
# &to240:10�ʿ�->240�ʿ�(0x10-0xFF)�ˤ���ؿ�
# 0x00-0x0F:���楳����

	if($nita == 0){$nita = 10;}
	if($nres == 0){$nres = 10;}

	$userver = '';
	$upath = '';
	$sbname = '';
	if($url =~ /^(h?ttp\:\/\/[^\/]+)(\/.+)$/){
		$userver = $1;
		$upath = $2;
		if($userver =~ /^h?ttp\:\/\/[0-9]+$/ || $userver =~ /[\.\/]2ch\.net/ || $userver =~ /[\.\/]bbspink\.com/ || $userver =~ /[\.\/]vip2ch\.com/ || $userver =~ /[\.\/]kakiko\.com/ || $userver =~ /[\.\/]machi\.to/ || $userver =~ /[\.\/]shiroro\.com/){	# 2ch��URL
			if($upath =~ /\.html/){	# HTML���ѤߤΥե�����ξ��
				puterror('0');
			}
			
			$upath =~ s|/i/|/|g;
			$upath =~ s|/read\.cgi/|/|g;
			$upath =~ s|/p\.i/|/|g;
			$upath =~ s|/r\.i/|/|g;
			$upath =~ s|/test/|/|g;
			if($upath =~ /([0-9]{9,10})/){	# �쥹��ɽ��
				$ithread = $1;
				$upath =~ s|/([0-9]{9,10})/|/|;
			}
			$upath =~ s|//+|/|g;
			if($userver =~ /^h?ttp\:\/\/([0-9]+)$/){	# http://���ֹ�/�����ֹ�/
				$iboard = $1;
				$str = &boradurl($iboard);
				$str =~ m|(http\://[^/]+)/(.+)/|;
				$userver = $1;	$sboard = $2;
			} elsif($userver =~ /imona\.2ch\.net/){		# http://imona.2ch.net/���ֹ�/�����ֹ�
				if($upath =~ /^\/([0-9]+)/){
					$iboard = $1;
					$str = &boradurl($iboard);
					$str =~ m|(http\://[^/]+)/(.+)/|;
					$userver = $1;	$sboard = $2;
				} else {
					puterror('0');
				}
			} elsif($upath =~ /\/([A-Za-z0-9]{2,12})\// ){	# ���̤�URL
				$sboard = $1;	# ��̾�����
				$iboard = url2nbrd('/' . $1 . '/',$userver);	# url�������ֹ�����
				#print $iboard;
				$upath =~ s|/([A-Za-z0-9]{2,12})/|/|;

				$str = &boradurl($iboard);	# �Ĥ�URL��Ƽ���
				if($str =~ m|(http\://[^/]+)/(.+)/|){
					$userver = $1;			# �İ����ˤ��륵���С�����Ѥ���(�Ť�url�����򤷤������Ĥ�URL��pc2�ؽ񤭴���äƤ��ޤ��Τ��ɤ�����)
				}
			} else {	# ��̾�������Ǥ��ʤ�
				puterror('0');
			}
			$upath =~ s|[0-9]/+$||;

			if($ithread != 0){	# �쥹��ɽ��
				if($upath =~ /\/(l?)([0-9]+)(\-?)([0-9]*)$/){	# �쥹���ֻ��꤬������
					if($1 ne ''){# last
						$last = $2;
						if($last > $nres){
							$last = $nres;
						}
					} else {
						if($3 ne ''){# -
							$start = $2;
							if($4 eq '' || $4 - $2 > $nres){
								$to = $start + $nres;
							} else {
								$to = $4;
							}
						} else {	# 1�쥹�����
							$start = $2;
							$to = $2;
						}
					}
				} else {
					$start = 1;	$to = $nres;
				}
			} else {	# �Ĥ�ɽ��
				if($ver >= 12){
					$sbname = getboardname($iboard);
				}
				$ithread = 0;
				$last = $nita;
			}
			
		} else {	# 2ch��URL�ϥ��顼
			puterror('0');
		}
	}
	
	# print "st:$start to:$to\n\n";	# debug
} elsif ($mode eq 'U'){	# ���ֹ椫���Ĥ�URL�����

	$str = &boradurl($iboard);
	if($str ne ''){
		&print_ua(1);

		$str =~ s|http://||;
		print $str;
	} else {
		puterror('0');
	}
	#exit();
	
} else {
	$str = &boradurl($iboard);
	if($str =~ m|(http\://[^/]+)/(.+)/|){
		$userver = $1;	$sboard = $2;
	} else {
		$userver = '';	$sboard = '';
	}
}

# kakolog
#if($iboard >= 3506 && $ver >= 11){
#	$rawmode = 0;
#}

# �ɤ߹����ϰϤ�Ĵ��
if($range eq '' && $nita == 0){$start=1;$to=10;}
if($start != 0 && $last == 0 && $to == 0){$to = $start + 10;}

$shitaraba = 0;
if($userver =~ /$hostshitaraba/){	# �������
	$shitaraba = 1;
	#$rawmode = 1;
}
$matibbs = 0;
if($userver =~ /$hostmatibbs/){	# �ޤ�BBS
	$matibbs = 1;
	require 'Mb.pl';
	#$rawmode = 1;
}



if($rawmode == 1 || $shitaraba == 1){	# rawmode
	require 'http.pl';

	$http'ua = 'iMona/1.0';
	$http'range = 0;
	$http'other = '';

	@http'header = ();

	if($last > 0){
		if($last <= 2){
			$last = 3;
		}
		if($shitaraba == 1){
			$ran = "l$last";
		} elsif($matibbs == 1){
			$ran = "l".($last - 1);
		}else {
			$ran = "\&ls=$last";
		}
	} else {
		if($mode eq 'm' && $start > 1){
			if($shitaraba == 1 || $matibbs == 1){
				if($start == 1){
					$ran = "1-$to";
				} else {
					$ran = ($start-1) . "-$to";
				}
			} else {
				if($start == 1){
					$ran = "\&st=1\&to=$to";
				} else {
					$ran = "\&st=" . ($start-1) . "\&to=$to";	# raw�⡼�ɤǤϡ�start�ǻ��ꤷ���ֹ�Υ쥹��¸�ߤ��ʤ�����
																# ���٤ƤΥǡ�������������Τ�ž���̤�̵�̤ʤ��ᡢ���������ꤷ�Ƥ�����
				}
			}
		} else {
			if($shitaraba == 1 || $matibbs == 1){
				$ran = "$start-$to";
			} else {
				$ran = "\&st=$start\&to=$to";
			}
		}
	}

	if($mode eq 's'){	# ����åɸ���
		$ran = '';
	}
} else {	# datľ
	if(!exists $ENV{MOD_PERL}){# || $fastcgi == 1){
		require '2c.pl';
	}

	$datst = $start;
	$datto = $to;
	$datls = $last;

	if($mode eq 's'){	# ����åɸ���
		$datst = 1;
		$datto = 1000;
		$datls = 0;
	}
}

if($iboard == 1108 && $ithread == 1){	# iMona�Υ���åɤθƤӽФ�
	$ithread = $imonathread;
}

if($rawmode == 1 && $iboard >= 9000 && $iboard < 10000){
	puterror('B');	# ���顼��Ϥ�
}

@data = ();
$buf = '';	$buf2 = '';	$buf3 = '';
$putdatatype = 0;

if($mode eq 'b'){		# �İ����Υ�������� board
	&boardlist();
	#exit();
} elsif($mode eq 'me'){	# ��˥塼���������
	&print_ua(1);

	if(open(DATA, "menu.txt")){
		binmode(DATA);
		while(<DATA>){
			$buf .= $_;
		}
		close(DATA);
		
		if($ver >= 4){
			print length($buf) . "\t" . length($buf) . "\n" . $buf;
		} else {
			print length($buf) . "\n" . $buf;
		}
	}
	#exit();
}

if($mode eq 'u' || $mode eq 't' || $mode eq 's' || $mode eq 'm'){	# URL��ɽ��or����åɰ���ɽ��or����åɸ���orǤ�ե���åɤ�Ǥ�ե쥹��ɽ��
	if($iboard < 8000 && $userver eq ''){puterror('0');}

	# ������Х��ݡ���
	if($shitaraba == 1){
		if($ithread == 0){	# ����åɰ���
			$str = $userver . "/$sboard/subject.txt";
			$data = &http'get($str);

			&jcode::convert(\$data, 'sjis' , 'euc'); # sjis���Ѵ�
			@data = split(/\n/, $data);
			&filter_shitaraba();	# shitaraba to 2ch
			&threadlist();
		} else {
			$str = $userver . "/bbs/rawmode.cgi/$sboard/$ithread/$ran";
			$data = &http'get($str);

			&jcode::convert(\$data, 'sjis' , 'euc'); # sjis���Ѵ�
			@data = split(/\n/, $data);
			&filter_shitaraba_res();	# shitaraba to 2ch
			&threadres();
		}
	# �ޤ�BBS
	} elsif($matibbs == 1){
#		if($ithread == 0){	# ����åɰ���
#			$str = $userver . "/bbs/offlaw.cgi/$sboard/";
#			$data = &http'get($str);
#
#			@data = split(/\n/, $data);
#			&filter_matibbs();
#			&threadlist();
#		} else {
#			$str = $userver . "/bbs/offlaw.cgi/$sboard/$ithread/$ran";
#			$data = &http'get($str);

#			@data = split(/\n/, $data);
#			&filter_matibbs_res();	# matibbs to 2ch
#			&threadres();
#		}
		$userver =~ s|http://||;
		$data = &pMbbscache'read( $userver, $sboard, $ithread, $datst, $datto, $datls);
		
		@data = split(/\n/, $data);
		if($data[0] =~ /LIST/){
				&threadlist();
			} else {
				&threadres();
		}
	# 2ch, 2ch�ߴ�
	} elsif($iboard < 8000 || ($iboard >= 9000 && $iboard < 10000)){
		if($rawmode == 1) {		#rawmode
			if($ithread == 0){	# ����åɰ���
				$str = $userver . "/test/read.cgi/$sboard/\?raw=0.0$ran";
				$data = &http'get($str);
				@data = split(/\n/, $data);
				&threadlist();
			} else {
				$str = $userver . "/test/read.cgi/$sboard/$ithread/\?raw=0.0$ran";
				$data = &http'get($str);
				@data = split(/\n/, $data);
				&threadres();
			}
		} else {				#datľ�ɤ�
			$userver =~ s|http://||;
			$data = &p2chcache'read( $userver, $sboard, $ithread, $datst, $datto, $datls);
			# be.2ch.net�����Ф�euc�ʤΤ�sjis���Ѵ�����
			if($userver =~ /be\.2ch\.net/){
				if($encodemod >= 1){	# Encode �⥸�塼�����Ѥ���
					#Encode::from_to($data,'euc-jp', 'shiftjis');	# sjis���Ѵ�
					$data = $euc_enc->decode($data);
					$data = $sjis_enc->encode($data);
				} else {
					&jcode::convert(\$data, 'sjis' , 'euc');		# sjis���Ѵ�
				}
			}
			@data = split(/\n/, $data);
			if($data[0] =~ /LIST/){
				&threadlist();
			} else {
				&threadres();
			}
		}
	# �ץ饰���� (8000 <= $iboard && $iboard < 9000)
	} else {
		#$putdatatype = 1;
		$pluginname = &getplugin($iboard);
		if($pluginname ne ''){
			require "$plugindir/$pluginname.pl";
			@data = @{eval("&$pluginname\:\:read( \$iboard, \$ithread, \$datst, \$datto, \$datls, \$FORM{'o'}, \$FORM{'f'});")};
		}
		#print "[[$data[0] $mode $putdatatype]]";
		if($data[0] =~ /LIST/){
			if($mode eq 'm'){
				$mode = 't';
			}

			&threadlist();
		} else {
			if($mode eq 't' || $mode eq 's'){
				$mode = 'm';
			}
			
			&threadres();
		}
	}
} elsif($mode eq 'w2'){	# ����Ω�� write

	# 0.73�ʹߤ���Ѥ��Ƥ���Ȳ��ꤹ�롣
	$brd2 = $brd4;
	$brd3 = $brd5;

#	if($ver eq '' || ($writemode == 0 && $rawmode == 1) || $writemode == 1){	# ������쥯��
#		$str =~ s|(http\://[^/]+/)([^/]+)/|$1test/r.i/$2/$ithread/w|;
#		
#		print "Location: $str\n\n";
#	} else {	# �񤭹��߲��̤ν���

		print "Content-type: text/html;charset=Shift_JIS\n\n";
		
		#$str =~ m|(http://[^/]+)/([^/]+)/|;
		#...ad...
		#require("../ad.pl");

		# ����åɥ����ȥ�ν���
		if($rawmode == 0){
			@data = split(/\n/, &p2chcache'read( $userver, $sboard, $ithread, 1, 1, 0));
			print ((split(/<>/, $data[1]))[4] . "<br>");
		}
		
		if ($imode == 1) {
			if ( $userver =~ /sports2\.2ch\.net/ ) {
				$buf = "<form method=post action=$userver/test/bbs.cgi?guid=ON utn>";
			} elsif ($userver =~ /[\.\/]2ch\.net/ || $userver =~ /[\.\/]bbspink\.com/) {
				$buf = "<form method=post action=$userver/test/bbs.cgi?guid=ON>";
			} else {
				$buf = "<form method=post action=$userver/test/bbs.cgi?guid=ON utn>";
			}
		} else {
			$buf = "<form method=post action=$userver/test/bbs.cgi>";
		}
		$buf .= "Name<input name=FROM size=14><br>Mail<input name=mail size=14><br>Subject<input name=subject size=14><br><textarea name=MESSAGE cols=20></textarea><input type=submit name=submit value=\xBD\xDA\x97\xA7>";
		if($kddi == 1){
			$buf .= '<a href="device:jam?MIDlet-Name=iMona&MIDlet-Vendor=soft.spdv.net&MIDlet-Version=7.7.0&">iMona</a>';
		}

		$buf .= "<hr><input name=bbs value=$sboard><input name=time value=" . (time - 300)  . "><input type=hidden name=get value=1></form>";
		
		print $buf;
#	}

} elsif($mode eq 'w'){	# �񤭹��� write
	&writelog();
	# 0.73�ʹߤ���Ѥ��Ƥ���Ȳ��ꤹ�롣
	$brd2 = $brd4;
	$brd3 = $brd5;

	
	if($ver eq '' || ($writemode == 0 && $rawmode == 1) || $writemode == 1 || $shitaraba == 1 || $matibbs == 1){	# ������쥯��
		#mod_perlư�����httpd.conf��PerlSendHeader On�ˤäƤ��ʤ��Ȥ���HTTP�إå�����Ϥ���
		#if(exists $ENV{MOD_PERL} && (!$ENV{PERL_SEND_HEADER} || $ENV{PERL_SEND_HEADER} !~ /^On/i)){
		#	print(($ENV{SERVER_PROTOCOL} || 'HTTP/1.0') . " 302 Found\r\n");
		#}

		if($shitaraba == 1){
			$str = $userver . "/bbs/i.cgi/$sboard/$ithread/w";
		} else {
			if($matibbs == 1){
				$str = $userver . "/bbs/read.pl?IMODE=TRUE&KEY=$ithread&BBS=$sboard&WRITEBOX=TRUE";
			}else{
				$str = &boradurl($iboard);
				$str =~ s|(http\://[^/]+/)([^/]+)/|$1test/r.i/$2/$ithread/w|;
			}
		}
		
		#print "Location: $str\n\n";

		# �С�����󤬻��ꤵ��Ƥ��ʤ��Ȥ��˵����ֹ�ǥ����å�->r.i������ʥ���Ϥʤ��Ȥ������顼��Ϥ����鿷���ֹ����Ѥ���
		#if($ver eq ''){
		#	#httpget($str);
		#	@data = split(/\n/, &http'get($str));
		#	if($data[0] =~ /ERR ?- ?480/ || $data[0] eq ''){
		#		$brd2 = $brd4;
		#		$brd3 = $brd5;

		#		$str = &boradurl($iboard);
		#		$str =~ s|(http\://[^/]+/)([^/]+)/|$1test/r.i/$2/$ithread/w|;
		#	}
		#}

		print "Location: $str\n\n";
	} else {	# �񤭹��߲��̤ν���
		#mod_perlư�����httpd.conf��PerlSendHeader On�ˤäƤ��ʤ��Ȥ���HTTP�إå�����Ϥ���
		#if(exists $ENV{MOD_PERL} && (!$ENV{PERL_SEND_HEADER} || $ENV{PERL_SEND_HEADER} !~ /^On/i)){
		#	print(($ENV{SERVER_PROTOCOL} || 'HTTP/1.0') . " 200 OK\r\n");
		#}

		print "Content-type: text/html;charset=Shift_JIS\n\n";
		
		#$str =~ m|(http://[^/]+)/([^/]+)/|;
		#...ad...
		require("../ads/smart.cgi");

	
		# ����åɥ����ȥ�ν���
		if($rawmode == 0){
			@data = split(/\n/, &p2chcache'read( $userver, $sboard, $ithread, 1, 1, 0));
			print ("<hr>".(split(/<>/, $data[1]))[4] ."<a href=http://i0.k2y.info/lr/lr.php?s=$userver&d=$sboard>\xDB\xB0\xB6\xD9\xD9\xB0\xD9</a>". "<br>");
		}
		
		$buf = "<hr>";
		if ($imode == 1) {
			if ( $userver =~ /sports2\.2ch\.net/ ) {
				$buf .= "<form method=post action=$userver/test/bbs.cgi?guid=ON utn>";
			} elsif ($userver =~ /[\.\/]2ch\.net/ || $userver =~ /[\.\/]bbspink\.com/) {
				$buf .= "<form method=post action=$userver/test/bbs.cgi?guid=ON>";
			} else {
				$buf .= "<form method=post action=$userver/test/bbs.cgi?guid=ON utn>";
			}
		} else {
			$buf .= "<form method=post action=$userver/test/bbs.cgi>";
		}		
		$buf .= "Name<input name=FROM size=14><br>Mail<input name=mail size=14 value=sage><br><textarea name=MESSAGE cols=16></textarea><input type=submit name=submit value=\x82\xA9\x82\xAB\x82\xB1\x82\xDE>";
		if($kddi == 1){
			$buf .= '<a href="device:jam?MIDlet-Name=iMona&MIDlet-Vendor=soft.spdv.net&MIDlet-Version=7.7.0&">iMona</a> ';
		}

		$buf .= "<br><a href=./2.cgi?v=D&m=w2&b=$iboard>\xBD\xDA\x97\xA7</a>";
        if($userver =~ /[\.\/]bbspink\.com/){
        	$buf .= " <a href=$userver/test/r.i/$sboard/$ithread/>r.i</a>";
        }
        else{
        	$buf .= " <a href=http://c.2ch.net/test/-/$sboard/$ithread/i>c.2ch</a>";
        }
        $userver2 = $userver;
		$userver2 =~ s|http://||g;
        $buf .= " <a href=http://c2ch.net/orz/orz.cgi/-/$userver2/$sboard/$ithread/i>orz</a>";
        $buf .= " <a href=rescopy.cgi?server=$userver&directory=$sboard&datno=$ithread>RC</a>";
        $title = (split(/<>/, $data[1]))[4];
        $title2 = $title;
		$title =~ s/(\W)/'%' . unpack('H2', $1)/eg;
		#$title =~ tr/ /%20/;

        $buf .= " 0.<a accesskey=0 href=http://i0.k2y.info/km/km.cgi?$title+$userver+$sboard+$ithread>\x8C\x67\x91\xD1\xD2\xC6\xAD\xB0</a>";

        $buf .= "<hr><textarea>$title2\r\n$userver/test/read.cgi/$sboard/$ithread/</textarea>";

		$buf .= "<hr><input name=key value=$ithread><input name=bbs value=$sboard><input name=time value=$ithread><input type=hidden name=get value=1></form>";

		#$buf .= "<hr><input name=key value=$ithread><input name=bbs value=$sboard><input name=time value=$ithread><input type=hidden name=get value=1></form>";


		
		print $buf;
	}

}
	#exit();

# ������gzip���̤�Ԥ�ʤ��Ȥ���1 content-type�������Ϥ��ʤ��Ȥ���2
sub print_ua {
	
	# mod_perlư�����httpd.conf��PerlSendHeader On�ˤäƤ��ʤ��Ȥ���HTTP�إå�����Ϥ���
	#if(exists $ENV{MOD_PERL} && (!$ENV{PERL_SEND_HEADER} || $ENV{PERL_SEND_HEADER} !~ /^On/i)){
	#	print(($ENV{SERVER_PROTOCOL} || 'HTTP/1.0') . " 200 OK\r\n");
	#}
	
	
	if($ENV{'HTTP_USER_AGENT'} =~ m/SoftBank/i){	# SoftBank
		print "Content-type: application/Java\n";
	} elsif($ENV{'HTTP_USER_AGENT'} =~ m|UNTRUSTED/\d\.\d|i || $ENV{'HTTP_USER_AGENT'} =~ m|Vodafone/.+? Java/|){	# Vodafone 3G
		print "Content-type: application/java\n";
	} elsif($ENV{'HTTP_USER_AGENT'} =~ m/PHONE\/[4-9]\./i || $ENV{'HTTP_USER_AGENT'} eq ''){# jphone P4 P5 W
		print "Content-type: application/Java\n";
	} elsif($ENV{'HTTP_USER_AGENT'} =~ m/PHONE\/3\./i){	# jphone C4
		print "Content-type: text/vnd.sun.j2me.app-descriptor\n";
	} elsif($ENV{'HTTP_USER_AGENT'} =~ m/DoCoMo/i){		# DoCoMo
		print "Content-type: text/plain\n";
	} elsif($ENV{'HTTP_USER_AGENT'} =~ m/KDDI/i){		# au
		print "Content-type: application/octet-stream\n";
	} else {
		if($mode eq 'src'){	
			print "Content-type: text/plain;charset=EUC-JP\n";
		}else{
			print "Content-type: text/plain\n";
		}
	}

	if($_[0] == 2){
		print "\n";
		return;
	}
	
	if($encgzip == 1 && $_[0] == 0 && $packmode != 0){
		if($ENV{'HTTP_ACCEPT_ENCODING'} =~ /x-gzip/){
			print "Content-Encoding: x-gzip\n\n";
		}else{
			print "Content-Encoding: gzip\n\n";
		}
	}else{
		print "\n";
	}

	if($ENV{'HTTP_USER_AGENT'} =~ m/PHONE\/3/i){		# jphone C4
		print "APDATA";
	}

	if($encgzip == 0 && $gzip > 0){						# gzip���̤���褦���ꤵ��Ƥ���
		if($_[0] == 1 || $packmode == 0){				# ���̤��ʤ��Ȥ�
			print "\x02";
		} else {
			if($forcecompress == 0){
				print "\x01";
			}
			if($sendpackedsize == 1){
				print &to240(length($output2)) . "\n";
			}
		}
	}

}

sub ime {
		my $ime;
		$ime = $_[0];
		
		if($ime =~ /[\/\.]2ch\.net|[\/\.]bbspink\.com/){
			return "$ime";
		} else {
			$ime =~ s/h?ttp\:\/\//http\:\/\/ime\.k2y\.info\/ime\.cgi\?/gi;
			return "$ime";
		}
}

sub threadres {	# �쥹��ɽ��

	if($rawmode == 1 && $data[0] =~ /Res:1-3\/3/ && $data[1] =~ /<><>[0-9\/: ]+ ID:Happy2chLife<>/){	# �����Ԥ�
		if($ver >= 11){
			puterror('D');	# ���顼��Ϥ�(�����Ԥ�)
			return;
		} else {
			puterror('B');	# ���顼��Ϥ�
			return;
		}
	}
	
	$counter = 0;
	$buf = '';	$buf2 = '';
	$stname = '';
	$local = 0;
	$datastatus = 0;
	foreach $str (@data) {
		
		if($counter == 0){	# 1���ܤ��ɤ���
			$counter++;

			if($str =~ /ERR - ([0-9]+)/i){
				$error = $1;
			} else {
				$error = 0;
			}
			
			if($error != 0 && $str !~ /Res\:/){	# ���顼
				if($error == 403){	# dat���
					puterror('D');	# ���顼��Ϥ�
					return;
				} elsif($error == 406){	# ���쥹��̵����
					puterror('A');	# ���顼��Ϥ�
					return;
				} else {
					puterror('B');	# ���顼��Ϥ�
					return;
				}
			} elsif($str =~ /Res:([0-9]+)\-([0-9]+)\/([0-9]+)/i){	# �Ϥ�-�����/�����ο�
				$counter = $1;
				$i = $1;
				$j = $2;
				$all = $3;
				
				if(0 < $last && $last < 3){		# raw�⡼�ɤ�ls<3�λ��˻��ꤷ�����ʾ�Υǡ���������Ƥ���ΤǤ����к���
					$start = $2 - $last + 1;
					$last = 0;
				}

				if($error == 405){				# �쥹�ΰ��֤λ��꤬���������Ȥ�(Ʃ������ˤ�뤺��ʤɤΥ��顼��ޤ�)
					if($reduceerror == 1){
						$start = $2 - ($to - $start);
						$to = $2;
						$i = $start;
						$last = 0;
					} else {
						puterror('B');			# ���顼��Ϥ�
						return;
					}
				}

				if($last == 0){
					if($start - 1 >= $1 && $start <= $2){
						$i = $start;
					} elsif($start == $1){		# �쥹��������
						#$i = $1;
					} elsif($start - 1 >= $1){	# ���쥹���ʤ��Ȥ�
						puterror('A');			# ���顼��Ϥ�
						return;
					} else {					# �ǡ����ΰ۾�
						puterror('B');			# ���顼��Ϥ�
						return;
					}
				}
					
				if($str =~ /\(([0-9]+)\)/){
					$datastatus = $1;
				}

				if($ver >= 9){
					if($datastatus != 0){
						$buf2 = &to240($i) . "\t" . &to240($j) . "\t" . &to240($all) . "\t" . &to240($datastatus) . "\n";
					} else {
						$buf2 = &to240($i) . "\t" . &to240($j) . "\t" . &to240($all) . "\n";
					}
				} else {
					$buf2 = "$i\t$j\t$all\n";
				}

			} elsif($rawmode == 1 && $http'header[0] =~ / 302 Found/) {	#��¿����
				puterror('E');	# ���顼��Ϥ�
				return;
			} else {			# �ǡ����ΰ۾�
				puterror('B');	# ���顼��Ϥ�
				#$buf2 = "ERROR($str)";	#debug
				return;
			}
			next;
		}

		#���������$counter > 0
		
		#$str =~ s/^([^\r\n]*)[\r\n]*$/$1/;
		if($str =~ /^\s*$/){next;}

		if($last == 0 && $counter < $start){$counter++;	next;}	# �ޤ��ǡ��������������֤ǤϤʤ����

		$str =~ s/\t/ /g;	# ���֤򥹥ڡ������Ѵ�
		
		$str =~ s/<>/\t/g;	# ���ڤ�ʸ����򥿥֤��Ѵ�

		#ID�ν���
		if($str =~ s/^([^\t]*\t[^\t]*\t[^\t]*) ID:([^\t]+?)\s*\t ?/$1\t$2\t/ == 0){
			# ID��¸�ߤ��ʤ����
			$str =~ s/^([^\t]*?\t[^\t]*\t[^\t]*\t)/$1\t/;
		}

		#$str =~ s/<> ?/\t/g;
		#print "Content-type: text/plain\n\n\n[$str]";

		# ���Ԥ��Ѵ�
		$str =~ s/ ?<br> ?/\r/gi;
		if($delret == 1){
			$str =~ s/\r//g;
		} elsif($delret == 2){
			$str =~ s/\r/ /g;
		}
		
		#�Х��к�
		#$str =~ s/((.*)<>){5}/$2/g;
		
		if($cushionpage == 1){
			$str =~ s/(h?ttps?:?[\x21-\x3B\x3D\x3F-\x7E]+)/&urlh($1)/eg;	#��ĥ���ץ����Σ�(r3�ˤ���������ǽ�ˤʤ롣
		}
		
		@resbuf = split(/ ?\t ?/, $str);
		#$resbli = $#resbuf;	# resbuf last index

		if($shitaraba != 1 && @resbuf < 3){$local = 1;}
		#if($matibbs != 1 && @resbuf < 3){$local = 1;}

		if($local == 1 || ($iboard >= 9000 && $iboard < 10000)){		# local
			if($resbuf[1] ne '' && $stname eq ''){	# ���ֻϤ�Υ쥹�λ�
				$stname = $resbuf[1];				# ����åɤΥ����ȥ����¸
			}
			$buf = $buf . "$resbuf[0]\n";
			$counter++;
			next;
		}

		#$str =~ s/\t([^\t]*)$/\t/;		# �Ǹ�ι���(�����ȥ�)����
		#if($1 ne '' && $stname eq ''){	# ���ֻϤ�Υ쥹�λ�
		if($resbuf[5] ne '' && $stname eq ''){	# ���ֻϤ�Υ쥹�λ�
			$stname = $resbuf[5];				# ����åɤΥ����ȥ����¸
		}

		if($delname == 1){
			#$str =~ s/^[^\t]*\t/\t/;
			$resbuf[0] = '';	# 0:name
		}
		if($delmail == 1){
			#$str =~ s/^([^\t]*\t)[^\t]*/$1/;
			$resbuf[1] = '';	# 1:mail
		}
		if($deltime == 1){
			#$str =~ s/^([^\t]*\t[^\t]*\t)[^\t]*/$1/;
			$resbuf[2] = '';	# 2:time
		}

		if($delid == 1){
			$resbuf[3] = '';
		}
		#ID�ˤ�뵡��ɽ��
		if($resbuf[3] =~ /^\s*\?+\s*$/){	#id��???�ʾ��
			$resbuf[3] = '';				#id��õ�
		}
		if($iboard !~ /100/){
			if(length($resbuf[3]) =~ /9/){
				if($resbuf[3] =~ /[^0OPQoIi]$/){	              #id��[0OPQo]�ʳ��ʾ��
					$resbuf[3] = "$resbuf[3]      Type:None"; #id��None���ɲ�
				}
				if($resbuf[3] =~ /0$/){	                    #id��0�ʾ��
					$resbuf[3] = "$resbuf[3]      Type:PC&Other"; #id��PC to Other���ɲ�
				}
				if($resbuf[3] =~ /O$/){	                                #id��phone to PHS�ʾ��
					$resbuf[3] = "$resbuf[3]      Type:mobilephone";#id��mobilephone&PHS���ɲ�
				}
				if($resbuf[3] =~ /P$/){	                           #id��P�ʾ��
					$resbuf[3] = "$resbuf[3]      Type:p2.2ch.net";#id��p2.2ch.net���ɲ�
				}
				if($resbuf[3] =~ /Q$/){	                           #id��Q�ʾ��
					$resbuf[3] = "$resbuf[3]      Type:middle2chbrowser";#id��middle2chbrowser���ɲ�
				}
				if($resbuf[3] =~ /o$/){	                           #id��o�ʾ��
					$resbuf[3] = "$resbuf[3]      Type:AIR-EDGEPHONE";#id��middle2chbrowser���ɲ�
				}
				if($resbuf[3] =~ /I$/){	                           #id��I�ʾ��
					$resbuf[3] = "$resbuf[3]      Type:DoCoMo";#id��Docomo���ɲ�
				}
				if($resbuf[3] =~ /i$/){	                           #id��I�ʾ��
					$resbuf[3] = "$resbuf[3]      Type:DoCoMo";#id��Docomo���ɲ�
				}
			}
		}
		#$resbuf[4] =~ s/(h?ttps?:?[\x21-\x3B\x3D\x3F-\x7E]+)/&urlh($1)/eg;
		
		#�����ե����ޥåȰ۾��к�
		#$resbuf[2] =~ s/ ([0-9]{2}:[0-9]{2})$/ $1:00/;

		if($kddi == 1 &&$ver <= 15){		# 0.77�°����΢�ɽ���Զ���к�
			$resbuf[3] =~ s/\x81\xF5/ Ktai/;
		}
		
		if($ver == 13){			# 0.76.1��ttp��󥯥Х��к�
			$resbuf[4] =~ s/^ttp/http/;
		}

		# AA��õ�
		if(($delaa == 2 && ($str =~ /   +/ || $str =~ /\x81\x40\x81\x40\x81\x40/ || $str =~ /\x81\x51\x81\x51\x81\x51/ || $str =~ /\x84\x9f\x84\x9f\x84\x9f/)) || ($delaa == 3 && ($str =~ /\x81\x40 /)) ){
			#my @aabuf = split(/\t/, $str);
			#my 4 = $#aabuf;
			my $outbuf = '';
			my ($len, $chrbuf, $chrbuf2, $sjis, $tag);
			$i = 0;
			$chrbuf = '', $chrbuf2 = '';
			$sjis = '';
			$tag = 0;
			
			# �Ѵ�
			$resbuf[4] =~ s/&gt;/>/g;
			$resbuf[4] =~ s/<a ?[^>]*?>(.*?)<\/?a>/$1/g;

			if($delurl > 0){	# URL��õ�
				$resbuf[4] =~ s/(h?ttps?:?[\x21-\x3B\x3D\x3F-\x7E]+)/&url($1)/eg;
			}

			$len = length($resbuf[4]);

			while(1){
				if($i >= $len){last;}

				$chrbuf = substr($resbuf[4], $i, 1);
				if($sjis eq '' && $chrbuf =~ /[\x81-\x9F\xE0-\xFE]/){	#sjis1�Х�����
					$sjis = $chrbuf;
				} elsif ($sjis ne '') {	#sjis2�Х�����
					if($sjis eq "\x81" && $chrbuf =~ /[\x40-\xFC]/){
						$outbuf .= ' ';
					} elsif($sjis eq "\x84" && $chrbuf =~ /[\x9F-\xBE]/){
					} else {
						$outbuf .= $sjis . $chrbuf;
					}
					$sjis = '';
				} else {	#1�Х���ʸ��
					if($chrbuf eq "\x3C"){
						$outbuf .= $chrbuf;
						$tag = 1;
					} elsif($chrbuf eq "\x3E"){
						$outbuf .= $chrbuf;
						$tag = 0;
					#} elsif($tag == 0 && $chrbuf =~ /[\x22-\x27\x2A-\x2F:;?\x5B-\x60\x7B-\x7E\xA0-\xA5]/){
					} elsif($tag == 0 && $chrbuf =~ /[\x22-\x27\x2A-\x2D;?\x5B-\x60\x7B-\x7E\xA0-\xA5]/){
							$outbuf .= ' ';
					} elsif($tag == 0 && $chrbuf2 !~ /[A-Za-z0-9]/ && $chrbuf =~ /[\x2E\x3A]/){	#.:
						$outbuf .= ' ';
					} else {
						$outbuf .= $chrbuf;
					}
					#} elsif($tag == 0 && $chrbuf =~ /[\xA0-\xA5]/){
					#	$outbuf .= ' ';
					#} elsif($tag == 1 || $chrbuf !~ /[\x22-\x27\x2A-\x2F:;?\x5B-\x60\x7B-\x7E]/){
					#	$outbuf .= $chrbuf;
				}

				$chrbuf2 = $chrbuf;

				$i++;
			}

			$resbuf[4] = $outbuf;
			#$resbuf[4] =~ s/([\x2E\x3A]){2,255}//g;
			$resbuf[4] =~ s/([\x2F]){3,255}//g;
			$resbuf[4] =~ s/  +/ /g;
			$resbuf[4] =~ s/\r\r+/\r/g;
			$resbuf[4] =~ s/ ([^\t ])/$1/g;
			
			#�ʲ����Դ�����
			#my(@resbuf) = split(/\t/, $str);
			#$resbuf[4] =~ s/&gt;/>/g;
			#$resbuf[4] =~ s/([\x00-\x80])\x81[\x40-\xFF]/$1 /g;
			#$resbuf[4] =~ s/\x81[\x40-\x80]/ /g;
			#$resbuf[4] =~ s/[\x22-\x27\x2A-\x2F:;?]//g;
			#$resbuf[4] =~ s/[^\x81-\x9F\xE0-\xFE][\x5B-\x60\x7B-\x7E]//g;
			#$resbuf[4] =~ s/  +/ /g;
			#$resbuf[4] =~ s/\r //g;
			#$resbuf[4] =~ s/ ([^\t ])/$1/g;
			#$str = "$resbuf[0]\t$resbuf[1]\t$resbuf[2]\t$resbuf[3]\t$resbuf[4]";
		} elsif(($delaa == 4 && ($str =~ /   +/ || $str =~ /\x81\x40\x81\x40\x81\x40/ || $str =~ /\x81\x51\x81\x51\x81\x51/ || $str =~ /\x84\x9f\x84\x9f\x84\x9f/)) || ($delaa == 5 && ($str =~ /\x81\x40 /)) ){
			#my @aabuf = split(/\t/, $str);
			#my 4 = $#aabuf;
			$resbuf[4] = '[AA]';
		} else {	#AA��õ�ʤ��Ȥ�
			#if($ver >= 8){	#ver8�ʹߤη������Ѵ� ����\t̾��\tMail\tTime\tID
				#$str =~ /^([^\t]*)\t([^\t]*)\t([^\t]*)\t([^\t]*)\t?([^\t]*)?$/;
				#if($5 eq ''){	#̾��\tMail\tTime\t���� -> ����\t̾��\tMail\tTime
				#	$str = "$4\t$1\t$2\t$3";
				#} else {	#̾��\tMail\tTime\tID\t���� -> ����\t̾��\tMail\tTime\tID
				#	$str = "$5\t$1\t$2\t$3\t$4";
				#}
			#}
		}

		if($resbuf[3] ne ''){	#id��������
			if($ver >= 8){	#ver8�ʹߤη����ˤ��� ����\t̾��\tMail\tTime\tID
				$str = "$resbuf[4]\t$resbuf[0]\t$resbuf[1]\t$resbuf[2]\t$resbuf[3]\n";
			} else {
				$str = "$resbuf[0]\t$resbuf[1]\t$resbuf[2]\t$resbuf[3]\t$resbuf[4]\n";
			}
		} else {
			if($ver >= 8){	#ver8�ʹߤη����ˤ��� ����\t̾��\tMail\tTime\tID
				$str = "$resbuf[4]\t$resbuf[0]\t$resbuf[1]\t$resbuf[2]\n";
			} else {
				$str = "$resbuf[0]\t$resbuf[1]\t$resbuf[2]\t$resbuf[4]\n";
			}
		}


		$buf = $buf . $str;

		$counter++;

	}	#end foreach
	
	if($buf eq ''){	#�ǡ���������ʤ����
		puterror('B');	#���顼��Ϥ�
		return;
	}
	
	if($mode eq 'u'){
		if($ver >= 12){
			if($ver >= 13){
				$output = "\x13";	#�쥹ɽ����(�����ȥ��դ�)
			} else {
				$output = "\x03";	#�쥹ɽ����(�����ȥ��դ�)
			}
			$stname =~ s/\n//g;
			$output .= &to240($iboard) . "\t" . &to240($ithread) . "\t" . $stname . "\n";
		} else {
			$output = "\x01";	#�쥹ɽ����
			$output .= &to240($iboard) . "\t" . &to240($ithread) . "\n";
		}
	} elsif($putdatatype == 1){
		$output = "\x13\n";	#��������ģ���(plain)
	}

	#�Ѵ�
	$buf =~ s/<\/?(b|ul)>//gi;
	$buf =~ s/<img[^>]* alt="?([^"]*)"?>/$1/gi;
	$buf =~ s/<\/?(font|table|tr|td|img) ?[^>]*?>//gi;

	$buf =~ s/ +([\n\r])/$1/g;	#���ڡ����ܲ��Ԥ���ԤΤߤˡ�

	# i2ch.net��dat����Ў׎����Ѥν���
	$buf =~ s|h?ttp://i2ch.net/z/---/mirror/|http://3509/|g;

	$buf =~ s/>0/>1/g;

	if($compression ne 'x'){	#���̤��ʤ�����ǤϤʤ��ä���
		#$buf = &compress($buf);
		&compress(*buf);
	} else {
		&convertprotcol($buf);
		&cnvemoji($buf);
		$lenbefore = length($buf);
	}
	
#	print length($buf) . "\t" . $buf2 . $buf;

	if($ver >= 9){
		$output .= &to240(length($buf)) . "\t" . &to240($lenbefore) . "\t" . $buf2 . $buf;
	} elsif($ver >= 4){
		$output .= length($buf) . "\t" . $lenbefore . "\t" . $buf2 . $buf;
	} else {
		$output .= length($buf) . "\t" . $buf2 . $buf;
	}

	&output;
}

#��ʸ���Ѵ�
sub cnvemoji{
	if($imode == 1){
		$_[0] =~ s/&hearts;/\xF8\xEE/gi;
	} elsif($kddi == 1){
		$_[0] =~ s/&hearts;/\xF7\xB2/gi;
	} else {#if($jphone == 1){
		$_[0] =~ s/&hearts;/\xE0\x22/gi;	#�ʎ���
	}
}

sub convertprotcol{
	if($delurl > 0){	#URL��õ�
		$_[0] =~ s/<a ?[^>]*?>(.*?)<\/?a>/$1/gi;

		$_[0] =~ s/(h?ttps?:?[\x21-\x3B\x3D\x3F-\x7E]+)/&url($1)/eg;
	} else {
		# ��󥯤ν���
		if($ver >= 15){	# �С������15�ʹ�
			$_[0] =~ s/<a [^>]*?href="?([^" >]*)"?[^>]*?>(.*?)<\/?a>/&ahreflink($1,$2)/gie;
			$_[0] =~ s/<input ([^>]*?)>/&inputobject($1)/gie;
		} else {
			#$_[0] =~ s/<a ?[^>]*?>(>>.*?)<\/?a>/$1/gi;
			#$_[0] =~ s/<a [^>]*?href="?([^" ]*)"?[^>]*?>.*?<\/?a>/$1/gi;
			$_[0] =~ s/<a [^>]*?href="?([^" >]*)"?[^>]*?>(.*?)<\/?a>/&ahreflinkold($1,$2)/gie;
			#$_[0] =~ s/<a ?[^>]*?>(.*?)<\/?a>/$1/gi;
		}

		$_[0] =~ s/h?ttps?:\/\/([0-9]+\/[\x21-\x3B\x3D\x3F-\x7E]+)/&url2($1)/eg;
	}

	$_[0] =~ s/&gt;/>/g;
	$_[0] =~ s/&lt;/</g;
	$_[0] =~ s/&quot;/"/g;
	$_[0] =~ s/&amp;/&/g;
	$_[0] =~ s/&nbsp;/ /g;
}

sub inputobject{
	my $str = shift;
	my @list;
	my $value, $size, $type;

	$type = "";
	$value = "";
	$size = 0;
	@list = split(/ /, $str);
	foreach $str (@list){
		if($str =~ /^value=(.+)$/){
			$value = $1;	$value	=~ s/%([a-fA-F0-9][a-fA-F0-9])/pack("C", hex($1))/eg;
		} elsif($str =~ /^size=(.+)$/){
			$size = $1;
		} elsif($str =~ /^type=(.+)$/){
			$type = $1;
		}
	}
	if($type eq "hidden"){						# hidden
		return "\x0E\x11" . &to240_2(length($value)) . $value;
	} if($type eq "" || $type eq "text"){		# �ƥ����ȥܥå���
		return "\x0E\x12" . &to240_2(length($value)) . $value . &to240_2($size);
	} elsif($type eq "checkbox"){				# �����å��ܥå���
		return "\x0E\x13" . &to240_2($value);
	} elsif($type eq "radio"){					# �饸���ܥå���
		return "\x0E\x14" . &to240_2($value);
	} elsif($type eq "submit"){					# ���֥ߥåȥܥ���
		return "\x0E\x1E" . &to240_2(length($value)) . $value;
	} elsif($type eq "reset"){					# �ե�����ζ��ڤ�
		return "\x0E\x1F" . &to240_2($value);
	}
}

sub ahreflink{
	my $linkurl = shift;
	my $linkstr = shift;

	$linkurl =~ s/^'(.+)'$/$1/;
	$linkstr =~ s/\s$//g;
	
	if($linkstr =~ /^http:\/\//){	# ���ʸ����http://�ǻϤޤ���
		return $linkstr;		# �����̤�Υ��
	} elsif($linkurl =~ /^\.\.\//) {	# 2ch�Υ���å�����
		return $linkstr;		# �����̤�Υ��
	} elsif($linkstr ne '') {	# ���ѥ��
		return "\x0C" . &to240_2(length($linkurl)) . $linkurl . &to240_2(length($linkstr)) . $linkstr;
	}
	return '';
}

sub ahreflinkold{
	my $linkurl = shift;
	my $linkstr = shift;

	$linkurl =~ s/^'(.+)'$/$1/;
	$linkstr =~ s/\s$//g;
	
	if($linkstr =~ /^http:\/\//){	# ���ʸ����http://�ǻϤޤ���
		return $linkstr;		# �����̤�Υ��
	} elsif($linkurl =~ /^\.\.\//) {	# 2ch�Υ���å�����
		return $linkstr;		# �����̤�Υ��
	} elsif($linkstr ne '') {	# ���ѥ��
		return $linkstr . "\r" . $linkurl;
	}
	return '';
}


sub puterror{	#���顼��Ϥ��ƽ�λ�������ϥ��顼�����ɡ�
#���顼�����ɤμ���
#A:�������쥹���ʤ�
#B:2ch�ؤ���³���顼
#C:�����ǲ���ҥåȤ��ʤ��ä�
#D:DAT���
#E:��¿��

	&print_ua(1);

	if($ver <= 12){	#�Х��Τ���
		if($mode eq 'u'){
			print "\x00";	#���顼
		}
	}

	if($ver >= 10){
		if($_[0] eq '0'){
			print "\x00\n";
		} else {
			print pack('C',unpack('C',$_[0]) - 64) . "\n";
		}
	} elsif($ver == 9){
		print "$_[0]\n";
	} else {
		print "0\n";
	}

	#exit();
}

sub url2 {
	my $str = $_[0];
	if($str =~ m/^([0-9]){9,10}/){
		return "http://imona.2ch.net/$iboard/$str";
	} else {
		return "http://imona.2ch.net/$str";
	}
}

#���å������
sub urlh {
	my $url = $_[0];
	$url =~ s/\#/\$/;
	if($url =~ /[\/\.]2ch\.net|[\/\.]bbspink\.com|[\/\.]kakiko\.com|[\/\.]psychedance\.com|[\/\.]3ch\.jp|[\/\.]vip2ch\.com|[\/\.]k2y\.info|[\.\/]shiroro\.com|[\.\/]machi\.to/){
		return "$url";
	}
	$url =~ s/h?ttp:\/\//http:\/\//;
	if($sp =~ /2/){
		if($ext =~ /all/i){
			$url =~ s/h?ttp:\/\///;
			return "http://imona.zuzu-service.net/ime.php?$url";
		}
		if($ext =~ /ima/i){
			if($url =~ /(\.?jpe?g|\.?png|\.?gif|\.?bmp)$/i){
				$url =~ s/h?ttp:\/\///;
				return "http://imona.zuzu-service.net/ime.php?$url";
			}
			return "$url";
		}
		if($ext == ""){
			$url =~ s/h?ttp:\/\///;
			return "$url";
		}
		if($url =~ /(\.?$ext)$/i){
			$url =~ s/h?ttp:\/\///;
			if($ext =~ /(html?)$/i){
				return "http://imona.zuzu-service.net/ime.php?$url";
			}
			return "http://imona.zuzu-service.net/ime.php?$url";
		}
		return "$url";
	}
	return "$url";
}


sub url {
	if($delurl == 3){	#�����õ�
		return "";
	} else {
		my $url = $_[0];
		if($url =~ /[\/\.]2ch\.net/){
			if($delurl == 2){	#2ch�ʳ���URL��õ�
				return "$url";
			} else {
				return '(2chURL)';
			}
		} elsif($url =~ /bbspink\.com/) {
			if($delurl == 2){	#2ch�ʳ���URL��õ�
				return "$url";
			} else {
				return '(bbspinkURL)';
			}
		} elsif($url =~ /\/\/(www)?\.?([^\/]*)\.[^\/.]*\//) {
			return "($2)";
		} elsif($url =~ m|://[0-9]+/|){
			if($delurl == 2){	#2ch�ʳ���URL��õ�
				return "$url";
			} else {
				return '(iMonaURL)';
			}
		}
		return "(URL)";
	}
}

#����åɰ�����ɽ��
sub threadlist {# subject.txt -> rawmode���ѹ��Ѥ�

#	foreach $str (@data) {
#		$str =~ s/^([^\r\n]*)[\r\n]*$/$1/;
#		if($str eq ""){next;}
#		if($str =~ m/.dat<>/){
#			$str =~ s/.dat<>/\t/;
#
#			push(@thread,$str);
#		}
#	}

#	if($range eq ''){$last = 10;}
#	if($last > 0){
#		$st = 0;
#		if(@line < $last){
#			$en = @line;
#		} else {
#			$en = $last;
#		}
#	} else {
#		$st = $start - 1;
#		$en = $to;
#	}
#	for($i=$st; $i<$en; $i++){
#		if($thread[$i] =~ /^([^\t]*)\t([^\t]*)\(([^\t]*)\)$/){
#			$buf = $buf . $1 . "\n";
#			$buf2 = $buf2 . $2 . "\n";
#			if($i == $en - 1){
#				$buf3 = $buf3 . $3;
#			} else {
#				$buf3 = $buf3 . $3 . "\n";
#			}
#		}
#		#$buf = $buf . "$thread[$i]\n";
#	}

#	local(*s) = @_;

#	@data = split(/\n/, $s);

	$str = '';
	@data2 = ();
	@tmp = ();

	$sword2 = '';#	$sword3 = '';
	if($sword ne ''){	#����������ɤ�¸�ߤ�����

		#$str = $s;
		$str = $data;
		#&jcode'convert(*str, 'euc' , 'sjis'); # euc���Ѵ�
		#&jcode'z2h_euc(*str);  # ���ѥ��ʤ�Ⱦ�ѥ��ʤ��Ѵ�
		#&jcode'sjis2euc(*str, 'h'); # euc���Ѵ�+���ѥ��ʤ�Ⱦ�ѥ��ʤ��Ѵ�

		if($encodemod >= 2){	# Drk::Encode �⥸�塼�����Ѥ���
			$str = $DrkEncode->z2h($str, "sjis");	# ����ʸ����Ⱦ�Ѥ��Ѵ�
			$str =~ tr/a-z/A-Z/;					# ��ʸ����ʸ���ζ��̤򤷤ʤ�����
		} else {
			if($encodemod >= 1){	# Encode �⥸�塼�����Ѥ���
				# euc���Ѵ�+���ѥ��ʤ�Ⱦ�ѥ��ʤ��Ѵ�
				#Encode::from_to($str,'shiftjis', 'euc-jp');
				$str = $sjis_enc->decode($str);
				$str = $euc_enc->encode($str);
				Encode::JP::H2Z::z2h(\$str);
			} else {
				&jcode::convert(\$str, 'euc' , 'sjis', 'h'); # euc���Ѵ�+���ѥ��ʤ�Ⱦ�ѥ��ʤ��Ѵ�
			}
			&jcode::tr(\$str, '��-����-�ڣ�-��a-z', '0-9A-ZA-ZA-Z');	# ���ѱѿ�����Ⱦ�ѱѿ�����ʸ�����Ѵ�����
		}
		$str =~ s/[^<\n]*?<>(.*?)\([^)]*?\)\n/$1\n/g;	#����åɥ����ȥ������ȴ���Ф�
		@data2 = split(/\n/, $str);

		#����������ɤ�Ⱦ�Ѳ������ѱѿ�����Ⱦ�Ѿ�ʸ����Ⱦ�ѱѿ�����ʸ�����Ѵ�
		$sword2 = $sword;
		#&jcode'convert(*sword2, 'euc' , 'sjis'); # euc���Ѵ�
		#&jcode'z2h_euc(*sword2);  # ���ѥ��ʤ�Ⱦ�ѥ��ʤ��Ѵ�
		#&jcode'sjis2euc(*sword2, 'h'); # euc���Ѵ�+���ѥ��ʤ�Ⱦ�ѥ��ʤ��Ѵ�
		if($encodemod >= 2){	# Drk::Encode �⥸�塼�����Ѥ���
			$sword2 = $DrkEncode->z2h($sword2, "sjis");	# ����ʸ����Ⱦ�Ѥ��Ѵ�
			$sword2 =~ tr/a-z/A-Z/;						# ��ʸ����ʸ���ζ��̤򤷤ʤ�����
		} else {
			if($encodemod >= 1){	# Encode �⥸�塼�����Ѥ���
				# euc���Ѵ�+���ѥ��ʤ�Ⱦ�ѥ��ʤ��Ѵ�
				#Encode::from_to($sword2,'shiftjis', 'euc-jp');
				$sword2 = $sjis_enc->decode($sword2);
				$sword2 = $euc_enc->encode($sword2);
				Encode::JP::H2Z::z2h(\$sword2);
			} else {
				&jcode::convert(\$sword2, 'euc' , 'sjis', 'h'); # euc���Ѵ�+���ѥ��ʤ�Ⱦ�ѥ��ʤ��Ѵ�
				#&jcode'tr(*sword2, '��-����-�ڣ�-��', '0-9A-Za-z');	# ���ѱѿ�����Ⱦ�ѱѿ������Ѵ�����
			}
			&jcode::tr(\$sword2, '��-����-�ڣ�-��a-z', '0-9A-ZA-ZA-Z');	# ���ѱѿ�����Ⱦ�Ѿ�ʸ����Ⱦ�ѱѿ�����ʸ�����Ѵ�����
		}

		@sword = split(/\s/, $sword2);

		#&jcode'h2z_euc(*sword3);  # Ⱦ�ѥ��ʤ����ѥ��ʤ��Ѵ�
		#&jcode'tr(*sword3, '0-9A-Za-z', '��-����-�ڣ�-��');	# Ⱦ�ѱѿ��������ѱѿ������Ѵ�����

		#&jcode'convert(*sword2, 'sjis' , 'euc'); # sjis���Ѵ�
		#&jcode'convert(*sword3, 'sjis' , 'euc'); # sjis���Ѵ�
	}

	$tnum = 0;
	$buf = '';	$buf2 = '';	$buf3 = '';
	thread: for($i = 0; $i <= $#data; $i++){
	#foreach $str (@data) {
		$str = $data[$i];
		if($i == 0){
			#$i = 1;

			if($sword ne ''){next;}
			if($str =~ /Res:([0-9]*)\-([0-9]*)\/([0-9]*)/){	#�Ϥ�-�����/�����ο�
				#$buf2 = "$1\t$2\t$3\n";
				if($last == 0 && $start != $1){
					puterror("0");	#���顼��Ϥ�
					return;
				}
			} elsif($rawmode == 1 && $http'header[0] =~ / 302 Found/) {	#��¿����
				puterror('E');	#���顼��Ϥ�
				return;
			} else {
				puterror('B');	#���顼��Ϥ�
				return;
			}
		} else {
			#$str =~ s/^([^\r\n]*)[\r\n]*$/$1/;
			#$str =~ s/\r?\n$/$1/;
			if($str eq ""){next;}

			if($sword ne ''){	#����
				#$str2 = $str;

				#if($str !~ m/$sword/i && $str !~ m/$sword2/i && $str !~ m/$sword3/i){next;}	#������
				#&jcode'convert(*str2, 'euc' , 'sjis'); # euc���Ѵ�
				#&jcode'z2h_euc(*str2);  # ���ѥ��ʤ�Ⱦ�ѥ��ʤ��Ѵ�
				#&jcode'tr(*str2, '��-����-�ڣ�-��', '0-9A-ZA-Z');	# ���ѱѿ�����Ⱦ�ѱѿ�����ʸ�����Ѵ�����
				#&jcode'convert(*str2, 'sjis' , 'euc'); # sjis���Ѵ�
				#if($matibbs == 1){
				#	foreach $sword2 (@sword){
				#		if(index($data2[$i-1], $sword2) == -1){next thread;}	#������ɤ����Ĥ���ʤ��Ȥ��ϼ���
				#	}
				#}else{
					foreach $sword2 (@sword){
						if(index($data2[$i], $sword2) == -1){next thread;}	#������ɤ����Ĥ���ʤ��Ȥ��ϼ���
					}
				#}
			}

			#if($str =~ m/.dat<>/){
			#$str =~ s/.dat<>/\t/;

			@tmp = split(/<>/, $str);

			#if($matibbs == 1){
			#	shift(@tmp);
			#}
			#if($str =~ /^(\w+)\.dat<>(.*) ?\((\w+)\)[\r\n]*$/){
			if(@tmp => 2){
				$tmp[0] =~ s/\.dat$//;
				if($ver >= 7){
					if(@tmp == 3){
						$tmp[2] =~ s/[\r\n]//g;
						
						$tmp[1] =~ /(.*)\s?\((\w+)\)$/;
						$buf .= $tmp[0] . "\t" . $1 . "\r" . $tmp[2] . "\t" . $2 . "\n";	#res num
					} else {
						$tmp[1] =~ /(.*)\s?\((\w+)\)[\r\n]*$/;

						#$buf = $buf . &to240($1) . "\t" . $2 . "\t" . &to240($3) . "\n";	#res num
						$buf .= $tmp[0] . "\t" . $1 . "\t" . $2 . "\n";	#res num
					}

					$tnum++;
					if($tnum >= 239){
						last;
					}
				} else {
					$tmp[1] =~ /(.*)\s?\((\w+)\)[\r\n]*$/;

					$buf = $buf . $tmp[0] . "\n";	#dat num
					$buf2 = $buf2 . $1 . "\n";	#thread name
					$buf3 = $buf3 . $2 . "\n";	#res num
				}
			}
		}
	}
	

	if($buf eq ''){	#�ǡ���������ʤ����
		if($sword ne '' && $ver >= 11){
			puterror('C');	#���顼��Ϥ�(no hit)
		} else {
			puterror('B');	#���顼��Ϥ�
		}
		return;
	}

	if($mode eq 'u'){
		if($ver >= 12){
			if($ver >= 13){
				$output = "\x14";	#��������ģ���(�����ȥ��դ�)
			} else {
				$output = "\x04";	#��������ģ���(�����ȥ��դ�)
			}
			$output .= &to240($iboard) . "\t" . $sbname . "\n";
		} else {
			$output = "\x02";	#��������ģ���
			$output .= &to240($iboard) . "\n";
		}
	} elsif($putdatatype == 1){
		$output = "\x14\n";	#��������ģ���(plain)
	}

	$buf3 =~s/ $//;
	
	if($ver < 7){
		$buf = $buf . "\n" . $buf2 . "\n" . $buf3;
	} elsif($ver == 7) {	#�Х��н�
		my $i;
		for($i = $tnum;$i < 10;$i++){
			$buf .= "1000000000\t \t1\n";
		}
		if($tnum < 10){$tnum = 10;}
	} elsif($ver > 7) {
		$tnum += 16;
	}
	

	$buf =~ s/&gt;/>/g;
	$buf =~ s/&lt;/</g;
	$buf =~ s/&quot;/"/g;
	$buf =~ s/&amp;/&/g;
	$buf =~ s/&nbsp;/ /g;
	$buf =~ s/ +([\n\r\t])/$1/g;	#���ڡ����ܲ��Ԥ���ԤΤߤˡ�
	
	#$buf = &compress($buf);
	&compress(*buf);
	#if($compression ne 'x'){	#���̤��ʤ�����ǤϤʤ��ä���
	#	$buf = &compress($buf);
	#} else {
	#	$lenbefore = length($buf);
	#}
	
	if($ver >= 9){
		$lenbefore++;
		$output .= &to240(length($buf)) . "\t" . &to240($lenbefore) . "\n" . chr($tnum) . $buf;
	} elsif($ver >= 7){
		$lenbefore++;
		$output .= length($buf) . "\t" . $lenbefore . "\n" . chr($tnum) . $buf;
	} elsif($ver >= 4){
		$output .= length($buf) . "\t" . $lenbefore . "\n" . $buf;
	} else {
		$output .= length($buf) . "\n" . $buf;
	}

	&output;

}

sub sjis2reg {
	local($j1,$j2) = @_;
	$j2 = "\\$j2" if $j2 =~ /[\[\\\]\{\|\}]/;
	$j1.$j2;
}

# 240�ʿ���
sub to240{
	my ($i, $ret);
	$i = $_[0], $ret = '';
	
	while($i > 0){
		$ret = chr($i % 240 + 16) . $ret;
		#$ret = ($i % 240 + 16) . " " .  pack('C',$i % 240 + 16) . " " . $ret;
		$i = int($i / 240);
	}
	return $ret;
}

# 2�Х��ȸ��� 240�ʿ���
sub to240_2{
	my $ret, $len;
	$ret = &to240($_[0]);
	$len = length($ret);

	if($len == 1){
		$ret = "\x10" . $ret;
	} elsif($len == 0){
		$ret = "\x10\x10";
	} elsif($len == 2){
	} else {
		$ret = substr($ret, $len - 2, 2);
	}
	
	return $ret;
}

#���������������
sub writelog{
	if(open(DATA, "+< writeLog.txt")){
		binmode(DATA);
		flock(DATA, 2);				# ��å���ǧ����å�
		
		$line = <DATA>;
		#close(DATA);
		
		@log = split(/\t/,$line);

		if($kddi == 1){
			$log[0]++;
		} elsif($imode == 1){
			$log[1]++;
		} elsif($jphone == 1){
			$log[2]++;
		} elsif($other == 1){
			$log[3]++;
		}

		#if(open(DATA, ">iMonaLog.txt")){
			#binmode(DATA);
		#truncate(OUT, 0);    # �ե����륵������0�Х��Ȥˤ���
		seek(DATA, 0, 0);			# �ե�����ݥ��󥿤���Ƭ�˥��å�
		print DATA "$log[0]\t$log[1]\t$log[2]\t$log[3]";

		close(DATA);
		#}
	}
}



sub log {
	if(open(DATA, "+< iMonaLog.txt")){
		binmode(DATA);
		flock(DATA, 2);				# ��å���ǧ����å�
		
		$line = <DATA>;
		#close(DATA);
		
		@log = split(/\t/,$line);

		if($kddi == 1){
			$log[0]++;
		} elsif($imode == 1){
			$log[1]++;
		} elsif($jphone == 1){
			$log[2]++;
		} elsif($other == 1){
			$log[3]++;
		}

		#if(open(DATA, ">iMonaLog.txt")){
			#binmode(DATA);
		#truncate(OUT, 0);    # �ե����륵������0�Х��Ȥˤ���
		seek(DATA, 0, 0);			# �ե�����ݥ��󥿤���Ƭ�˥��å�
		print DATA "$log[0]\t$log[1]\t$log[2]\t$log[3]";

		close(DATA);
		#}
	}
}

sub boardlist {	#�Ĥ�ɽ��
	my ($i, @line);

	if($ver >= 15){
		if(! open(DATA, "$flexiblebrd")){
			return;
		}
	} else {
		if(! open(DATA, "$brd2")){
			return;
		}
	}
	
	binmode(DATA);
	while(<DATA>){
		$buf .= $_;
		#if($ver >= 15 && /^[\r\n]+$/){last;}
	}
	
	#$i = 0;
	#if($ver >= 15){
	#	while(<DATA>){
	#		if($ver >= 15 && $i % 2 == 1){
	#			chomp;
	#			@line = split( /\t/, $_ );
	#			foreach $line (@line){
	#				$line = &to240($line);
	#			}
	#			$_ = join( "\t", @line) . "\n";
	#		}
	#		$buf .= $_;
	#		$i++;
	#	}
	#}

	close(DATA);

	if($ver >= 12){
		$buf =~ s/\r//g;
	}

#		if($compression ne 'x'){	#���̤��ʤ�����ǤϤʤ��ä���
		#$line[$i] = &compress($line[$i]);
#			$buf = &compress($buf);
#		} else {
		$lenbefore = length($buf);
#		}
	
	if($ver >= 12){
		$output .= &to240(length($buf)) . "\t" . &to240($lenbefore) . "\n" . $buf;
	} elsif($ver >= 4){
		$output .= length($buf) . "\t" . $lenbefore . "\n" . $buf;
	} else {
		$output .= length($buf) . "\n" . $buf;
	}

	&output;
}

sub url2nbrd{	#URL�����ֹ���Ѵ�
	my ($i, @line, @line2, $url);
	
	#VIPURLDAT����к�
	#if($_[0] == "news4vip"){
	#	$_[0]
	#}
	$url = $_[1].$_[0];
	if($#brd5cache < 0 || $ver < 11){
		if(open(DATA, "$brd3")){
			binmode(DATA);
			@line = <DATA>;
			close(DATA);
		} else {
			return -1;
		}
	} else {
		@line = @brd5cache;
	}
	
	for($i = 0;$i <= $#line;$i++){
		if($line[$i] =~ /^(.*?)$url/){
			@line2 = split(/\t/, $1);
			if($line[$i] =~ /$_[1]/){
				if($#line2 < 0){
					return $i * 100;
				} else {
					#return $i * 100 + ($#line2 - 1);
					return $i * 100 + ($#line2 + 1);
				}
			}
		}
	}
	return -1;
}

sub getboardname {	#�Ĥ��ֹ椫����̾�����
	if(open(DATA, "$brd2")){
		binmode(DATA);
		while(<DATA>){if($_ =~ /^[\r\n]+$/){last;}}	#���ԤΤߤιԤޤǤϤ���ʤ�
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

sub boradurl {	#�Ĥ��ֹ椫��URL�����
	$i = 0;
	@line = ();

	if($_[0] >= 1000000 && $_[0] < 2500000){
		my @category = ('auto','computer','game','movie','music','shop','sports','travel','business','study','news','otaku','anime','comic','school');

		return "http://$hostshitaraba/" . $category[int($_[0] / 100000 - 10)] . "/" . $_[0] % 100000 . "/";
	} elsif($_[0] >= 9000 && $_[0] < 10000){
		return "http://local/$_[0]/";
	}

	$i = int($_[0] / 100);

	if($#brd5cache < 0 || $ver < 11){
		if(open(DATA, "$brd3")){
			binmode(DATA);
			@line = <DATA>;
			close(DATA);

			$line[$i] =~ s/[\r\n]*$//;
		} else {
			return '';
		}
		
		if($line[$i] =~ /\t/){
			@array = split( /\t/, $line[$i]);
			return $array[$_[0] - $i * 100];
		} else {
			return $line[$i];
		}
	} else {	#�İ���������å��夵��Ƥ�����
		@line = @brd5cache;

		return $brd5splitcache[$i][$_[0] - $i * 100];
	}
	return '';

	#���δؿ��Ǥ����ʲ�����ʤ�
	
	#	$j = 0;
	#	for($i=0; $i<@line; $i++){
	#		$line[$i] =~ s/^([^\r\n]*)[\r\n]*$/$1/;
	#		if($line[$i] eq ""){
	#			next;
	#		}

	#		if($compression ne 'x'){	#���̤��ʤ�����ǤϤʤ��ä���
	#			$line[$i] = &compress($line[$i]);
	#		}
			
	#		$line[$i] =~ m/^([^\t]*)\t?([^\t]*)$/;
	#		if($2 ne ''){
	#			if($j == $_[0]){
	#				return $2;
	#			}
	#			$j++;
	#		}
	#	}
	
	#return '';
}

sub getformdata {
	my $i = 0;
	$buffer = '';

	if ($ENV{'REQUEST_METHOD'} eq "POST") {
		read(STDIN, $buffer, $ENV{'CONTENT_LENGTH'});
	} else {
		$buffer = $ENV{'QUERY_STRING'};
	}

	@buffer = split(/&/, $buffer);
	%FORM = ();

	foreach $pair (@buffer) {
		$i = index($pair, '=');
		$name = substr($pair, 0, $i);
		$value = substr($pair, $i+1);
		#local($name, $value) = split(/=/, $pair);
		$name	=~ tr/+/ /;
		$name	=~ s/%([a-fA-F0-9][a-fA-F0-9])/pack("C", hex($1))/eg;
		$value	=~ tr/+/ /;
		$value	=~ s/%([a-fA-F0-9][a-fA-F0-9])/pack("C", hex($1))/eg;
		#$value	=~ s/</&lt;/ig;
		#$value	=~ s/>/&gt;/ig;
		#$value	=~ s/\r\n/<br>/g;
		#$value	=~ s/\n/<br>/g;
		#$value	=~ s/\r/<br>/g;
		#$value	=~ s/\,/&#44;/g;
		$FORM{$name} = $value;
	}
}

sub compress {	#�ʰץǡ�������
	my $i = 0;
#	$data = $_[0];
	local(*data) = @_;

	$data =~ s/\xFA\x44/v/g;

	#if($ENV{'HTTP_USER_AGENT'} =~ m/P504i/){	#`->���Ѵ�
	#	&jcode'tr(*data, '`',"\x81\x4D");	#\x81\x4D = '��'(SJIS)
	#}

	#��ĵհ���
	if($compressmode != 2){
		#$sjis  = '[\x80-\x9F\xE0-\xF7\xFA-\xFC][\x40-\x7E\x80-\xFC]|[\x00-\x7F]|[\xA1-\xDF]';
		#$emoji = '[\xF8\xF9][\x40-\x7E\x80-\xFC]';
		#$gaiji = '[\xF0-\xFC][\x40-\x7E\x80-\xFC]';
		#if($imode == 1){		#imode��ʸ���Ѵ�
		#	$data =~ s/\G((?:$sjis)*)($emoji)/$1.'&#'.unpack('n',$2).';'/eg;#o;
		#} else {
		#	$data =~ s/\G((?:$sjis)*)($emoji)/$1=/g;#o;
		#}
		#$data =~ s/\G((?:$sjis)*)($gaiji)/$1=/g;#o;	#������'='���Ѵ�
		

		#&jcode'convert(*data, 'euc' , 'sjis'); # euc���Ѵ�
		#&jcode'convert(*data, 'euc' , 'sjis', 'h'); # euc���Ѵ�+���ѥ��ʤ�Ⱦ�ѥ��ʤ��Ѵ�
		#&jcode'sjis2euc(*data, 'h'); # euc���Ѵ�+���ѥ��ʤ�Ⱦ�ѥ��ʤ��Ѵ�

		if($encodemod >= 2){	# Drk::Encode �⥸�塼�����Ѥ���
			# ��ʸ���Υ���������
			$data = $DrkEncode->imode_enc($data);

			if($ENV{'HTTP_USER_AGENT'} =~ m/P504i/){	#`->���Ѵ�
				# euc���Ѵ�+���ѥ��ʤ�Ⱦ�ѥ��ʤ��Ѵ�
				#Encode::from_to($data,'shiftjis', 'euc-jp');
				$data = $sjis_enc->decode($data);
				$data = $euc_enc->encode($data);
				Encode::JP::H2Z::z2h(\$data);

				&jcode::tr(\$data, '`',"\x81\x4D");	#\x81\x4D = '��'(SJIS)
				
				&jcode::tr(\$data, '��-����-�ڣ�-���ɡ������ǡá�����ʡˡСѡΡϡ����ܡ����䡩��������', '0-9A-Za-z"#$%&\'|\\^! (){}[]:;+*=<>?/_@-');

				#Encode::from_to($data,'euc-jp', 'shiftjis');	# sjis���Ѵ�
				$data = $euc_enc->decode($data);
				$data = $sjis_enc->encode($data);
			} else {
				$data = $DrkEncode->z2h($data, "sjis");
			}
		} elsif($encodemod >= 1){	# Encode �⥸�塼�����Ѥ���
			$data = &replace_gaiji($data);
			
			# euc���Ѵ�+���ѥ��ʤ�Ⱦ�ѥ��ʤ��Ѵ�
			#Encode::from_to($data,'shiftjis', 'euc-jp');
			$data = $sjis_enc->decode($data);
			$data = $euc_enc->encode($data);
			Encode::JP::H2Z::z2h(\$data);

			if($ENV{'HTTP_USER_AGENT'} =~ m/P504i/){	#`->���Ѵ�
				&jcode::tr(\$data, '`',"\x81\x4D");	#\x81\x4D = '��'(SJIS)
			}

			&jcode::tr(\$data, '��-����-�ڣ�-���ɡ������ǡá�����ʡˡСѡΡϡ����ܡ����䡩��������', '0-9A-Za-z"#$%&\'|\\^! (){}[]:;+*=<>?/_@-');

			#Encode::from_to($data,'euc-jp', 'shiftjis');	# sjis���Ѵ�
			$data = $euc_enc->decode($data);
			$data = $sjis_enc->encode($data);
		} else {
			$data = &replace_gaiji($data);

			&jcode::convert(\$data, 'euc' , 'sjis', 'h'); # euc���Ѵ�+���ѥ��ʤ�Ⱦ�ѥ��ʤ��Ѵ�

			if($ENV{'HTTP_USER_AGENT'} =~ m/P504i/){	#`->���Ѵ�
				&jcode::tr(\$data, '`',"\x81\x4D");	#\x81\x4D = '��'(SJIS)
			}
			#&jcode'z2h_euc(*data);  # ���ѥ��ʤ�Ⱦ�ѥ��ʤ��Ѵ�
			&jcode::tr(\$data, '��-����-�ڣ�-���ɡ������ǡá�����ʡˡСѡΡϡ����ܡ����䡩��������', '0-9A-Za-z"#$%&\'|\\^! (){}[]:;+*=<>?/_@-');	# �ʲ����Ĥ����
			#&jcode'tr(*data, '��-����-�ڣ�-��', '0-9A-Za-z');	# ���ѱѿ�����Ⱦ�ѱѿ������Ѵ�����
			#&jcode'tr(*data, '�ɡ������ǡá�����ʡˡСѡΡϡ����ܡ����䡩��������', '"#$%&\'|\\^! (){}[]:;+*=<>?/_@-');	# ���ѥ��ڡ����ʤɤ�Ⱦ�ѥ��ڡ����ʤɤ��Ѵ�����

			#&jcode'convert(*data, 'sjis' , 'euc'); # sjis���Ѵ�
			#&jcode'euc2sjis(*data); # sjis���Ѵ�
			&jcode::convert(\$data, 'sjis' , 'euc'); # sjis���Ѵ�
		}

		$data =~ s/ +\n/\n/g;	#���ڡ���+����-�����
		
		if($encodemod >= 2){	# Drk::Encode �⥸�塼�����Ѥ���
			#if($imode == 1){		#imode��ʸ���Ѵ�
				$data = $DrkEncode->imode_dec($data);
			#}
		} else {
			#if($imode == 1){		#imode��ʸ���Ѵ�
				$data =~ s/&#(63\d{3});/pack('n',$1)/eg;
			#}
		}
	} elsif($ENV{'HTTP_USER_AGENT'} =~ m/P504i/){
		&jcode::convert(\$data, 'euc' , 'sjis'); # euc���Ѵ�
		#&jcode'sjis2euc(*data); # euc���Ѵ�

		#`->���Ѵ�
		&jcode::tr(\$data, '`',"\x81\x4D");	#\x81\x4D = '��'(SJIS)

		&jcode::convert(\$data, 'sjis' , 'euc'); # sjis���Ѵ�
		#&jcode'euc2sjis(*data); # sjis���Ѵ�
	}

	&convertprotcol($data);
	&cnvemoji($data);

	#if($ENV{'HTTP_USER_AGENT'} =~ m/P504i/){
	#	$data =~ s/[^\x81-\x9F\xE0-\xFE]\x60/\x81\x4D/g;
	#}

	$lenbefore = length($data);
	
	if(($mode eq 't' || $mode eq 's' || ($mode eq 'u' && $ithread == 0))){	#����åɰ����ο�����򰵽�
		if($ver >= 7){
			$data =~ s/([^\t]*)\t([^\t]*)\t([^\t\n]*)\n/&to240($1) . "\t" . $2 . "\t" . &to240($3) . "\n"/eg;
		}
		$data =~ s/[\r\n]*$//;
	}

	#if($compression ne 'x'){	#���̤��ʤ�����ǤϤʤ��ä���
	if($compressmode == 2 || $compressmode == 0){	#�ĵհ��̤�Ԥ����
		if($ver >= 6){
			#�ĵհ���3	�Ҥ餬��/*��������*/����
			$data =~ s%((\x82[\x9F-\xFF]){4,254})%"\x08" . pack('C',length($1) / 2) . &hirakata_compress($1)%gem;

			#�ĵհ���1	ñ��ʼ���
			$i = 0;
			foreach $str (@compw) {
				$data =~ s/$str/pack('C', $i)/ge;
				$i++;
			}

			#�ĵհ���2	����󥰥�
			#4ʸ���ʾ��Ϣ³����ʸ��������󥰥����̤���
			if($ver >= 10){
				$data =~ s/([\x00-\xFF])(\1{2,254})/"\x0B" . pack('C', length($2) + 1) . "$1"/gem;
			} else {
				$data =~ s/([\x00-\xFF])(\1{3,254})/"\x07\x01" . pack('C', length($2) + 1) . "$1"/gem;
			}
			#$data =~ s/([\x00-\xFF]{4,255}?)(\1{2,254})/"\x07" . pack('C',length($1)) . pack('C',(length($2) \/ length($1) + 1) ) . "$1"/gem;
			$data =~ s/([\x00-\xFF]{4,64}?)(\1{2,254})/"\x07" . pack('C',length($1)) . pack('C',(length($2) \/ length($1) + 1) ) . "$1"/gem;
		} elsif($ver >= 5){
			#�ĵհ���3	�Ҥ餬��/*��������*/����
			$data =~ s%((\x82[\x9F-\xFF]){4,254})%"\x08" . pack('C',length($1) / 2) . &hirakata_compress($1)%gem;

			#�ĵհ���2	����󥰥�
			#4ʸ���ʾ��Ϣ³����ʸ��������󥰥����̤���
			$data =~ s/([\x00-\xFF])(\1{3,254})/"\x07\x01" . pack('C', length($2) + 1) . "$1"/gem;
			#$data =~ s/([\x00-\xFF]{4,255}?)(\1{2,254})/"\x07" . pack('C',length($1)) . pack('C',(length($2) \/ length($1) + 1) ) . "$1"/gem;
			$data =~ s/([\x00-\xFF]{4,64}?)(\1{2,254})/"\x07" . pack('C',length($1)) . pack('C',(length($2) \/ length($1) + 1) ) . "$1"/gem;
			#$data =~ s/([\x00-\xFF]{4,255}?)(\1{1,254})/&compress_sub( $1, $2)/gem;

			#�ĵհ���1	ñ��ʼ���
			$i = 0;
			foreach $str (@compw) {
				$data =~ s/$str/pack('C', $i)/ge;
				$i++;
			}
		} else {
			#�ĵհ���1	ñ��ʼ���
			foreach $str (@compw) {
				if($ver == 1 && $i >= 4){
					last;
				}
				$data =~ s/$str/pack('C', $i)/ge;
				$i++;
			}

			#�ĵհ���3	�Ҥ餬��/*��������*/����
			#if($ver >= 4){
			#	#$data =~ s%(((\x82[\x9F-\xFF])|(\x83[\x00-\x9E])){4,255})%"\x08" . pack('C',length($1) / 2) . &hirakata_compress($1)%gem;
			#	$data =~ s%((\x82[\x9F-\xFF]){4,255})%"\x08" . pack('C',length($1) / 2) . &hirakata_compress($1)%gem;
			#}

			#�ĵհ���2	����󥰥�
			if($ver >= 2){
				#$data =~ s/(([\x00-\xFF]{4, 255}?){2, 255})/"\x07" . pack('C',length($2)) . "$2" . (length($1) \/ length($2))/ge;	#4ʸ���ʾ��Ϣ³����ʸ����
				#$data =~ s/(....+?)(\1+)/"\x07" . pack('C',length($1)) . "$1" . pack('C',(length($2) \/ length($1) + 1) )/ge;	#4ʸ���ʾ��Ϣ³����ʸ����
				#$data =~ s/(....+?)(\1{1,255})/"\x07" . pack('C',length($1)) . "$1" . pack('C',(length($2) \/ length($1) + 1) )/ge;	#4ʸ���ʾ��Ϣ³����ʸ����
				#$data =~ s/(.{4,255}?)(\1{1,254})/"\x07" . pack('C',length($1)) . "$1" . pack('C',(length($2) \/ length($1) + 1) )/gem;
				#4ʸ���ʾ��Ϣ³����ʸ��������󥰥����̤���
				$data =~ s/([\x00-\xFF])(\1{3,254})/"\x07\x01" . "$1" . pack('C', length($2) + 1)/gem;
				#$data =~ s/([\x00-\xFF]{4,255}?)(\1{1,254})/&compress_sub( $1, $2)/gem;
				#$data =~ s/([\x00-\xFF]{4,255}?)(\1{1,254})/"\x07" . pack('C',length($1)) . "$1" . pack('C',(length($2) \/ length($1) + 1) )/gem;
				$data =~ s/([\x00-\xFF]{4,64}?)(\1{1,254})/"\x07" . pack('C',length($1)) . "$1" . pack('C',(length($2) \/ length($1) + 1) )/gem;
			}
		}
	}

	return $data;
}

sub hirakata_compress {		# �Ҥ餬�ʥ������ʰ��̥���
	my($tmp1) = $_[0];
	
	$tmp1 =~ s/[\x82]([\x9F-\xFF])/$1/gem;
	#$tmp1 =~ s/[\x82]([\x9F-\xFF])/pack('C', unpack("C", $1) - 0x9F)/gem;
	#$tmp1 =~ s/[\x83]([\x00-\x9E])/pack('C', unpack("C", $1) + \x21)/gem;
	return $tmp1;
}

sub compress_sub {			# ����󥰥����̥���
	my($tmp1) = $_[0];
	my($tmp2) = $_[1];
	my($count) = length($tmp2) / length($tmp1) + 1;

	$tmp1 =~ s/([\x00-\xFF]{2,255}?)(\1{2,254})/"\x07" . pack('C',length($1)) . "$1" . pack('C',(length($2) \/ length($1) + 1) )/gem;

	# �ĵհ���3	�Ҥ餬��/*��������*/����
	#$tmp1 =~ s%((\x82[\x9F-\xFF]){4,255})%"\x08" . pack('C',length($1) / 2) . &hirakata_compress($1)%gem;
	
	return "\x07" . pack('C',length($tmp1)) . "$tmp1" . pack('C',$count);
}


sub replace_gaiji {
	#local(*old) = @_;	# �Ѵ�����ʸ����
	my $new = '';		# �Ѵ����ʸ����

	if ($_[0] =~ /[\xF0-\xFC]/) {	# ������ޤ��ǽ��������Ȥ����������򤷤ޤ�
		while (1) {
			if ($imode == 1 && $_[0] =~ s/^[\xF8\xF9][\x40-\x7E\x80-\xFC]//) {	# ��ʸ����λ�
				$new .= '&#' . unpack('n', $&) . ';';	# 10�ʿ�ɽ���ˤ��� $new ��Ϣ��
			} elsif ($_[0] =~ s/^[\xF0-\xFE][\x40-\x7E\x80-\xFC]//) {	# ������'='���Ѵ�
				$new .= '=';
			} elsif ($_[0] =~ s/^([\x81-\x9F\xE0-\xEF][\x40-\x7E\x80-\xFC])+//) {	# ��Ƭ�˳����ʳ��� Shift_JIS 2�Х���ʸ���󤬤����
				$new .= $&;
			} elsif ($_[0] eq ''){	# $_[0] ���ʤ��ʤä���
				last;	
			} else {	# ����ʳ��ξ��
				$_[0] =~ s/^([\x00-\xFF])//;
				$new .= $1;
			}
		}
	} else {
		return $_[0];	# �����Ϥʤ��ΤǤ��Τޤ޽���
	}
	return $new;
}


sub output {

	if($packmode == 0 || $gzip == 0){	# ���̤��ʤ�
		&print_ua(1);
		print $output;
	} else {
		if($packmode == 1){				# zlib����ѡ�
			if(!exists $ENV{MOD_PERL}){# || $fastcgi == 1){
				require Compress::Zlib;
			}
			if($deflate == 1) {
				my $x = Compress::Zlib::deflateInit(-Level => $gzip) or die;

				my ($out, $stat) = $x->deflate($output);
				$output2 = $out;
				$stat == Z_OK or die;
				
				($out, $stat) = $x->flush();
				$output2 .= $out;
				$stat == Z_OK or die;
			} else {
				$output2 = Compress::Zlib::memGzip($output);
			}

			#�ʲ��ϰ��̥�٥����ꤷ�褦�Ȥ��������ɤ������ʤ�������Ǥ��ʤ�gzip���Ǥ�������ΤǤ�᤿��
			#(�ɤ����zlib�����Υե����뤬�Ǥ����������͡�Zlib.pm��memGzip�򻲹ͤˤ����Τˤʤ�����)
			#(����Ǥ���gzip�Υإå��μ���0x78 0xDA���եå�������0x70 0xAF 0x48 0x29�Ȥ������ߤ��Ĥ���)
			#$x->deflate($output);�ν������Ƥ���������
			#my $x = Compress::Zlib::deflateInit(-Level => $gzip) or die;

			# minimal gzip header
			#$output2 = pack("C10", 0x1f, 0x8b, 0x08, 0,0,0,0,0,0, 0x03);

			#my ($out, $stat) = $x->deflate($output);
			#$output2 .= $out;
			#$stat == Z_OK or die;
			
			#($out, $stat) = $x->flush();
			#$output2 .= $out;
			#$stat == Z_OK or die;

			#$output2 .= pack("V V", Compress::Zlib::crc32($output), $x->total_in());
		} else {
			$| = 1;

			#�ե�����򤹤Ǥ˳��������֤�Ÿ�����Ƥ����Ĥ���⡼��
			#������$unzipmode = 2�λ��ϡ�windows+anhttpd�Ķ��ǤϤ��ޤ�ư���ʤ��ä���
			#$| = 1�ˤ��Ƥ���ˤ⤫����餺��close(FILE);��������open (UNZIP,"gzip -dc $tmpf |");
			#���Ƥ�ե���������Ƥ�gzip�˹ԤäƤ��ʤ����͡�

			if (open(FILE, "+< $tmpf2")) {	# Ÿ���Ѥ˰���ե��������¸ �ɤ߽񤭥⡼�ɤǳ���
			} else {open(FILE, ">$tmpf2");}

			binmode(FILE);
			if($win9x == 0){
				flock(FILE, 2);				# ��å���ǧ����å�
			}
			seek(FILE, 0, 0);			# �ե�����ݥ��󥿤���Ƭ�˥��å�
			$len = length($output);
			syswrite(FILE, $output, $len);
			truncate(FILE, $len);	# �ե����륵������񤭹�����������ˤ���

			if($packmode == 3){
				close(FILE);				# close����м�ư�ǥ�å����
			}

				# Ÿ��
				$output2 = '';
				open (UNZIP,"gzip -$gzip -c $tmpf2 |");
				binmode(UNZIP);
				while(<UNZIP>){
					$output2 .= $_;
				}
				close (UNZIP);

			if($packmode == 2){
				close(FILE);				# close����м�ư�ǥ�å����
			}

			$| = 0;

			#������ʲ���gzip�Υإå���ʤ��Ƥ���
			$flag = substr($output2, 2, 1);
			if($flag eq "\x08"){	#FLG.FNAME
				$output2 =~ s/(.{10}).*?\x00/$1/;
				$output2 = "\x1F\x8B\x08\x00" . substr($output2, 4);
			}

			if($deflate == 1) {
				$output2 = "\x78\xDA" . substr($output2, 10, length($output2) - 18) . &adler32(*output);
			}
		}

		if($zip == 1){
			require 'gz2pkz.pl';
			$gz2pkz'filename = "a";
			$output2 = &gz2pkz'convert(*output2);
		} else {
			if($nogzipheader == 1){
				$output2 = substr($output2, 10);
			}
		}

		if($forcecompress == 0 && length($output) <= length($output2)){	# ���̤��礭���ʤäƤ��ޤä����
			&print_ua(1);
			print $output;
		} else {
			&print_ua(0);
			print $output2;
		}
	}
}


sub filter_shitaraba {
	my $i, $n;
	@data2 = @data;
	@data = ();

	$data[0] = "Res:$start-$to/" . ($#data+1) . "\n";	# �إå�(Res:x-y/all)
	$n = 1;
	for($i = $start-1; $i < $to; $i++){
		$data2[$i] =~ s/\.cgi,/.dat<>/;
		$data2[$i] =~ s/\r\n/\n/;
		$data[$n] = $data2[$i];
		$n++;
	}
}

sub filter_shitaraba_res {
	my $i, $j, $n, $title, $shist, $shito;
	my @res;
	@data2 = @data;
	@data = ();
	$title = "";
	$shist = 0;	$shito = 0;

	@res = split(/<>/, $data2[$#data2]);
	if($last > 0){
		if($res[0] - $last > 0){
			$data[0] = "Res:" . ($res[0] - $last + 1) ."-$res[0]/$res[0]\n";	# �إå�(Res:x-y/all)
			$shist = $res[0] - $last + 1;	$shito = $res[0];
		} else {
			$data[0] = "Res:1-$res[0]/$res[0]\n";								# �إå�(Res:x-y/all)
			$shist = 1;	$shito = $res[0];
		}
	} else {
		if($start == 1){
			$data[0] = "Res:1-$res[0]/9999\n";									# �إå�(Res:x-y/all)
			$shist = 1;	$shito = $res[0];
		} else {
			$data[0] = "Res:" . ($start-1) . "-$res[0]/9999\n";					# �إå�(Res:x-y/all)
			$shist = $start-1;	$shito = $res[0];
		}
	}
	$n = 1;
	for($i = 0; $i <= $#data2; $i++){
		$data2[$i] =~ s/[\r\n]//g;
		@res = split(/<>/, $data2[$i]);
		if($res[0] == 1){$title = $res[5];}
		if($res[0] < $shist){next;}
		if($res[0] > $shito){last;}
		if($res[0] != $shist + $n - 1){											# �ֹ椬����Ǥ���Ȥ�
			for($j = 0; $j < $res[0] - ($shist + $n - 1); $j++){
				$data[$n] = "\x82\xA0\x82\xDA\x81\x5B\x82\xF1<><>???<><>";			# ���ܡ���
				$n++;
			}
		}
		if($res[6] ne ''){
			$data[$n] = "$res[1]<>$res[2]<>$res[3] $res[6]<>$res[4]<>$res[5]";
		} else {
			$data[$n] = "$res[1]<>$res[2]<>$res[3]<>$res[4]<>$res[5]";
		}
		$n++;
	}

	if($title ne ''){
		$data[1] .= $title;
	}

	#print "Content-type: text/plain\n\n\n";
	#print "@data";
}

sub filter_matibbs {
	my $i, $n, $mc;
	@data2 = @data;
	$mc = $#data;
	@data = ();
#	open(OUT,"> dabug.txt");
	$data[0] = "Res:$start-$to/" . ($mc) . "\n";	# �إå�(Res:x-y/all)
	$n = 1;
#	print OUT $data[0];
	if($sword eq ''){
		for($i = $start-1; $i < $to; $i++){
			#if($n == 1){
				$data2[$i] =~ s/^[0-9]+?<>([0-9]{10})<>/$1.dat<>/;
			#}else{
			#	$data2[$i] =~ s/^[0-9]+?<>([0-9]{10})<>/\n$1.dat<>/;
			#}
			#$data2[$i] =~ s/^([0-9]+?)<>/$1.dat<>/;
			$data2[$i] =~ s/\(([0-9]+?)\)$/ ($1)/;
			#$data2[$i] =~ s/\r\n/\n/;
			#print OUT $data2[$i];
			$data[$n] = $data2[$i];
			$n++;
		}
	}else{
		for($i = 0; $i < $mc; $i++){
			#if($n == 1){
				$data2[$i] =~ s/^[0-9]+?<>([0-9]{10})<>/$1.dat<>/;
			#}else{
			#	$data2[$i] =~ s/^[0-9]+?<>([0-9]{10})<>/\n$1.dat<>/;
			#}
			#$data2[$i] =~ s/^([0-9]+?)<>/$1.dat<>/;
			$data2[$i] =~ s/\(([0-9]+?)\)$/ ($1)/;
			$data2[$i] =~ s/\r\n/\n/;
			$data[$n] = $data2[$i];
#			print OUT $data[$n];
			$n++;
		}
	}
#	close(OUT); 

}

sub filter_matibbs_res {
	my $i, $j, $n, $title, $matist, $matito;
	my @res;
	@data2 = @data;
	@data = ();
	$title = "";
	$shist = 0;	$shito = 0;

	@res = split(/<>/, $data2[$#data2]);
	if($last > 0){
		if($res[0] - $last > 0){
			$data[0] = "Res:" . ($res[0] - $last + 1) ."-$res[0]/$res[0]\n";	# �إå�(Res:x-y/all)
			$matist = $res[0] - $last + 1;	$matito = $res[0];
		} else {
			$data[0] = "Res:1-$res[0]/$res[0]\n";								# �إå�(Res:x-y/all)
			$matist = 1;	$matito = $res[0];
		}
	} else {
		if($start == 1){
			$data[0] = "Res:1-$res[0]/9999\n";									# �إå�(Res:x-y/all)
			$matist = 1;	$matito = $res[0];
		} else {
			$data[0] = "Res:" . ($start-1) . "-$res[0]/9999\n";					# �إå�(Res:x-y/all)
			$matist = $start-1;	$matito = $res[0];
		}
	}
	$n = 1;
	for($i = 0; $i <= $#data2; $i++){
		@res = split(/<>/, $data2[$i]);
		if($res[0] == 1){$title = $res[5];}
		if($res[0] < $matist){next;}
		if($res[0] > $matito){last;}
		if($res[0] != $matist + $n - 1){											# �ֹ椬����Ǥ���Ȥ�
			for($j = 0; $j < $res[0] - ($matist + $n - 1); $j++){
				$data[$n] = "\x82\xA0\x82\xDA\x81\x5B\x82\xF1<><>???<><>";			# ���ܡ���
				$n++;
			}
		}
		$data2[$i] =~ s/^[0-9]+?<>//;
		$data[$n] = $data2[$i];
		$n++;
	}
	#print "Content-type: text/plain\n\n\n";
	#print "@data";
}



sub getplugin {
	my $brd, $tmp, @line, @buf;
	
	$brd = $_[0];

	if(open(DATA, "$plugindir/$plugin")){
		binmode(DATA);
		@line = <DATA>;
		close(DATA);

		foreach $tmp (@line){
			chomp($tmp);
			@buf = split(/,/, $tmp);
			if($buf[0] == $brd){
				return $buf[1];
			}
		}
	}
	return '';
}



#define BASE 65521 /* largest prime smaller than 65536 */

#/*
#   Update a running Adler-32 checksum with the bytes buf[0..len-1]
# and return the updated checksum. The Adler-32 checksum should be
# initialized to 1.
#Usage example: 
#
#
#   unsigned long adler = 1L;
#
#   while (read_buffer(buffer, length) != EOF) {
#     adler = update_adler32(adler, buffer, length);
#   }
#   if (adler != original_adler) error();
#*/

#unsigned long update_adler32(unsigned long adler, unsigned char *buf, int len)
sub adler32 {
	local(*buf) = @_;
	my($adlar, $s1, $s2, $n, $len);
	#$adlar = 1;
	
	#$s1 = $adler & 0xffff;
	#$s2 = ($adler >> 16) & 0xffff;
	$s1 = 1;	$s2 = 0;

	$len = length($buf);

	for ($n = 0; $n < $len; $n++) {
		$s1 = ($s1 + unpack( "x" . $n . " C", $buf)) % 65521;#BASE;
		$s2 = ($s2 + $s1) % 65521;#BASE;
	}
	
	return pack("n2", $s2, $s1);#pack("N", ($s2 << 16) + $s1);
}
