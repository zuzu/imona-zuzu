
# HTTP�̿��饤�֥��
#         soft.spdv.net

# �����http�ǥǡ�����������뤿��δʰץ饤�֥��Ǥ���
# gzip��Ÿ���������ॢ���Ƚ����򥵥ݡ��Ȥ��Ƥ��ޤ���
# mod_perl�Ǥλ��ѻ��Ǥϥۥ��ȤΥ�������֤���¸���Ƥ������������������Ǥ��뤳�Ȥǥ���åȤ�̵�̻Ȥ��򸺤餷�ޤ���
# 2ch�Ȥ��̿��Ѥ˺������줿��ΤʤΤǡ�¾�Υ����С��ǤϤ��ޤ�ư���ʤ����⤷��ޤ���

#��������ˡ
# require 'http.pl';
# $data = &http'get('http://www.yahoo.co.jp/');

# http://�Ͼ�ά���뤳�Ȥ�����ޤ���

#������
# �쥹�ݥ󥹥إå��ϡ�@http'header�˳�Ǽ����ޤ��������������ԤϾõ��Ƥ��ޤ���

package mhttp;

use Socket;

BEGIN {	#���ư���Τ�
	%downinfo = ();	# �ۥ��ȥ��������ν����
}

##/������ʬ/########################

do 'setting.pl';

$ua = 'iMona/1.0';	#USER-AGENT
#$ua = 'Monazilla/1.00 (toolname/ver)';	#USER-AGENT
#$ua = 'Monazilla/1.00 (iMona/1.0)';	#USER-AGENT

$range = 0;		#��������ɤ����ϰϤ���ꤹ����(Range: bytes=$range-)

$other = '';	#http�ꥯ�����Ȥ�Ǥ�դΥǡ������ɲä�����˻��Ѥ��ޤ���

$method = 'GET';

$http_log = '';#'http_log.txt';	# �̿���Υ�(�����ȼ��ʤ�)

####################################


sub get {	#HTTP�ǥǡ������������ɤ��롣
	$/ = "\x0A";	#���ԥ����ɤ�\x0A(LF)�ˤ��롣

	@header = ();
	$buffer = '';
	
	$port = 80;

	if($_[0] eq ''){return;}
	$_[0] =~ m/^(http\:\/\/)?([^\/]+)(\/.*)$/;
	
	$host = $2;
	$path = $3;

	if($host =~ m/^([^:]+):([^:]+)$/){
		$host = $1;
		$port = $2;
	}

	if (&checkdown($host) == -1) {
		#warn "downhost was found, host:[$host] downinfo:[$downinfo{$host}]";
		return;
	}

#	if($host eq 'school2.2ch.net'){
#		$ipaddress = "\x40\x47\xB1\xA2";
#	} else {
		$ipaddress = inet_aton("$host");
#	}

	if($ipaddress eq ''){
		&founddown($host);
		warn "ipaddress is NULL, host:[$host] url:[$_[0]]";
		return;
	}

	if($http_log ne ''){
		&print_http_log(time . " $_[0] [${range}-]\n");
	}

	#�ݡ����ֹ��IP���ɥ쥹�򥻥åȤ�����¤�Τ��롣 
	$address = pack_sockaddr_in($port,$ipaddress); 

	$proto = getprotobyname('tcp');
	socket(SOCKET,PF_INET,SOCK_STREAM,$proto);

	# �ե�����ϥ�ɥ� SOCKET ��Хåե���󥰤��ʤ�
	select(SOCKET); $|=1; select(STDOUT);
	binmode(SOCKET);

#alarm�����

	if($windows == 0){
		#perl 5.6�Υ����ʥ��������Ѥ���
		$ENV{PERL_SIGNALS} = "unsafe";
		
		$SIG{'ALRM'} = sub {die "timeout"};	# eval��die
		alarm($timeout);
	}

	eval{
		connect(SOCKET,$address);

		#print SOCKET "$method $path HTTP/1.1\r\n";
		print SOCKET "$method $path HTTP/1.0\r\n";
		print SOCKET "Host: $host\r\n";
		print SOCKET "User-Agent: $ua\r\n";
		if($range != 0){
			print SOCKET "Range: bytes=${range}-\r\n";
		}
		print SOCKET "Accept: */*\r\n";
		print SOCKET "Accept-Charset: *\r\n";
		print SOCKET "Accept-Language: ja\r\n";
		if($usegzip == 1){
			print SOCKET "Accept-Encoding: gzip\r\n";
		}
		print SOCKET "Connection: close\r\n";
		if($other ne ''){
			print SOCKET $other;
		} else {
			print SOCKET "\r\n";
		}
	};

	if($windows == 0){
		alarm(0);
		if($@ =~ m/timeout/){	#�����ॢ���Ȥ������
			&founddown($host);
			return '';
		}
	}

	if($windows == 0){
		$SIG{'ALRM'} = sub {die "timeout2"};	# eval��die
		if($timeout2 == 0){$timeout2 = $timeout;}
		alarm($timeout2);
	}
	
	eval{
		@line = <SOCKET>;
	};

	if($windows == 0){
		alarm(0);
		if($@ =~ m/timeout/){	#�����ॢ���Ȥ������
			return '';
		}
	}

	close (SOCKET);
	
	# �С��ܥ�ϥ������ϥ����������ʤ�
	if (index($line[0], " 403 ") >= 0) {
		&founddown($host);
	} elsif (index($line[0], " 302 ") >= 0) {
		for (@line) {
			if (m%/qb6.2ch.net/_403/%) {
				&founddown($host);
			}
		}
	}

	#@data = ();
	$ishead = 0;
	$gzip = 0;
	$chunked = 0;
	foreach $line (@line) {
		if ($ishead == 0) {
			$line =~ tr/\x0D\x0A//d;

			if ($line eq '') {$ishead = 1; next;}	#�إå���ʬ���ɤ߹��߽�λ

			push(@header, $line);

			if (index($line, ": gzip") >= 0) {$gzip = 1;}	#gzip�ǰ��̤��줿�ǡ����������Ƥ������
			if (index($line, "Transfer-Encoding: chunked") >= 0) {
				$chunked = 1;
			}
		} else {
			if($chunked == 1 && $line =~ /^[0-9A-Fa-f]+[ \x0D\x0A]*$/){	#���ʤ�Ŭ���ʽ���
				$buffer =~ s/\x0D\x0A$//;
				next;
			}
			if($_[1] == 0){
				@res = split(/<>/, $line);
				#if($res[0] == 1){$title = $res[5];}
				#if($res[0] < $matist){next;}
				#if($res[0] > $matito){last;}
				if($res[0] != $n){											# �ֹ椬����Ǥ���Ȥ�
					while ($res[0] != $n) {
						$buffer .= "\x82\xA0\x82\xDA\x81\x5B\x82\xF1<><>???<><>\n";			# ���ܡ���;
						$n++;
					}
				}
				$line =~ s/^[0-9]+?<>//;
				$line =~ s/(.*?<>.*?<>.*?)<>(.*?)<>/$1<> $2 <>/;
				$n++;
				
			}elsif($_[1] == 1){
				$line =~ s/^[0-9]+?<>([0-9]{10})<>/$1.dat<>/;
				$line =~ s/\(([0-9]+?)\)$/ ($1)/;
				$line =~ s/\r\n/\n/g;
				#chomp $line;
				#$line .= "\n";
			}

			$buffer .= $line;
			#if($gzip == 1){
			#	$buffer .= $line;
			#} else {
			#	$line =~ tr/\x0D\x0A//d;
			#	push(@data, $line);
			#}
		}
	}

	if($gzip == 1){

		#zlib����ѡ�
		if($unzipmode == 1){
			require Compress::Zlib;
			$buffer = Compress::Zlib::memGunzip($buffer);
			#@data = split(/\n/, $buffer);
		} else {
			$| = 1;

			#�ե�����򤹤Ǥ˳��������֤�Ÿ�����Ƥ����Ĥ���⡼��
			#������$unzipmode = 2�λ��ϡ�windows+anhttpd�Ķ��ǤϤ��ޤ�ư���ʤ��ä���
			#$| = 1�ˤ��Ƥ���ˤ⤫����餺��close(FILE);��������open (UNZIP,"gzip -dc $tmpf |");
			#���Ƥ�ե���������Ƥ�gzip�˹ԤäƤ��ʤ����͡�

			if (open(FILE, "+< $tmpf")) {	# Ÿ���Ѥ˰���ե��������¸ �ɤ߽񤭥⡼�ɤǳ���
			} else {open(FILE, ">$tmpf");}

			binmode(FILE);
			if($win9x == 0){
				flock(FILE, 2);				# ��å���ǧ����å�
			}
			seek(FILE, 0, 0);			# �ե�����ݥ��󥿤���Ƭ�˥��å�
			$len = length($buffer);
			syswrite(FILE, $buffer, $len);
			truncate(FILE, $len);	# �ե����륵������񤭹�����������ˤ���

			if($unzipmode == 3){
				close(FILE);				# close����м�ư�ǥ�å����
			}

				# Ÿ��
				$buffer = '';
				open (UNZIP,"gzip -dc $tmpf |");
				binmode(UNZIP);
				while(<UNZIP>){
					#$buffer = $_;
					#chomp($buffer);	push(@data, $buffer);
					$buffer .= $_;
				}
				close (UNZIP);

			if($unzipmode == 2){
				close(FILE);				# close����м�ư�ǥ�å����
			}

			$| = 0;
		}
	}
	
	$buffer;
	$buffer =~ s/(.+?)\n//;

	return $buffer;
	#return @data;
}

# �����󤷤Ƥ���ۥ��Ȥ����Ĥ��ä����������󤷤����֤����ꤹ�롣
sub founddown {
	$downinfo{"$_[0]"} = time();
}

# �ۥ��ȤΥ�������֤�Ĵ�٤롣
# ��������Τ��Ƥ���10ʬ�����̵���ǥ�����������Ǥ��롣
sub checkdown {
	if(time() - $downinfo{"$_[0]"} < 60 * 10) {
		return -1;
	}
	return 0;
}

sub print_http_log {
	if (open( HTTPLOG_RW, ">>$http_log")) {
		if($win9x == 0){
			flock(HTTPLOG_RW, 2);				# ��å���ǧ����å�
		}
		binmode(HTTPLOG_RW);

		print HTTPLOG_RW $_[0];
		close(HTTPLOG_RW);
	}
}

1;