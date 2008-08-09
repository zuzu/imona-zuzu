
# HTTP�ʐM���C�u����
#         soft.spdv.net

# �����http�Ńf�[�^���擾���邽�߂̊ȈՃ��C�u�����ł��B
# gzip�̓W�J�A�^�C���A�E�g�������T�|�[�g���Ă��܂��B
#
# mod_perl�ł̎g�p���ł͐ڑ���z�X�g�̏�Ԃ�ۑ����Ă����A�_�E�����̃A�N�Z�X���Ւf���邱�Ƃ�
# �X���b�g�̕s�v�Ȑ�L�𖳂����A2ch�̃T�[�o����K�͂Ƀ_�E���������ɂł�������x�̉^�p���\�ł��B
#
# 2ch�Ƃ̒ʐM�p�ɍ쐬���ꂽ���̂Ȃ̂ŁA���̃T�[�o�ł͂��܂������Ȃ���������܂���B

#���g�p���@
# require 'http.pl';
# $data = &http'get('http://www.yahoo.co.jp/');

# http://�͏ȗ����邱�Ƃ��o���܂��B

#���d�l
# ���X�|���X�w�b�_�́A@http'header�Ɋi�[����܂����A����������s�͏�������Ă��܂��B

package http;

use Socket;

BEGIN {	#����N�����̂�
	%downinfo = ();	# �z�X�g�_�E�����̏�����
}

##/�ݒ蕔��/########################

do 'setting.pl';

$ua = 'iMona/1.0';	#USER-AGENT
#$ua = 'Monazilla/1.00 (toolname/ver)';	#USER-AGENT
#$ua = 'Monazilla/1.00 (iMona/1.0)';	#USER-AGENT

$range = 0;		#�_�E�����[�h����͈͂��w�肷��ꍇ(Range: bytes=$range-)

$other = '';	#http���N�G�X�g�ɔC�ӂ̃f�[�^��ǉ�����ꍇ�Ɏg�p���܂��B

$method = 'GET';

####################################


sub get {	#HTTP�Ńf�[�^���_�E�����[�h����B
	$/ = "\x0A";	#���s�R�[�h��\x0A(LF)�ɂ���B

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
		warn "ipaddress is NULL, host:[$host] url:[$_[0]]";
		return;
	}

	#�|�[�g�ԍ���IP�A�h���X���Z�b�g�����\���̂����B 
	$address = pack_sockaddr_in($port,$ipaddress); 

	$proto = getprotobyname('tcp');
	socket(SOCKET,PF_INET,SOCK_STREAM,$proto);

	# �t�@�C���n���h�� SOCKET ���o�b�t�@�����O���Ȃ�
	select(SOCKET); $|=1; select(STDOUT);
	binmode(SOCKET);

#alarm���g�p

	if($windows == 0){
		#perl 5.6�̃V�O�i���������g�p����
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
		print SOCKET "Accept-Language: jp\r\n";
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
		if($@ =~ m/timeout/){	#�^�C���A�E�g�����ꍇ
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
		if($@ =~ m/timeout/){	#�^�C���A�E�g�����ꍇ
			return '';
		}
	}

	close (SOCKET);

	#@data = ();
	$ishead = 0;
	$gzip = 0;
	$chunked = 0;
	foreach $line (@line) {
		if ($ishead == 0) {
			$line =~ tr/\x0D\x0A//d;

			if ($line eq '') {$ishead = 1; next;}	#�w�b�_�����̓ǂݍ��ݏI��

			push(@header, $line);

			if($line =~ /\: gzip/){$gzip = 1;}	#gzip�ň��k���ꂽ�f�[�^�������Ă����ꍇ
			if($line =~ /Transfer-Encoding: chunked/){$chunked = 1;}

		} else {
			if($chunked == 1 && $line =~ /^[0-9A-Fa-f]+[ \x0D\x0A]*$/){	#���Ȃ�K���ȏ���
				$buffer =~ s/\x0D\x0A$//;
				next;
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

		#zlib���g�p�B
		if($unzipmode == 1){
			require Compress::Zlib;
			$buffer = Compress::Zlib::memGunzip($buffer);
			#@data = split(/\n/, $buffer);
		} else {
			$| = 1;

			#�t�@�C�������łɊJ������ԂœW�J���Ă�����郂�[�h
			#������$unzipmode = 2�̎��́Awindows+anhttpd���ł͂��܂������Ȃ������B
			#$| = 1�ɂ��Ă���ɂ�������炸�Aclose(FILE);����O��open (UNZIP,"gzip -dc $tmpf |");
			#���Ă��t�@�C���̓��e��gzip�ɍs���Ă��Ȃ��͗l�B

			if (open(FILE, "+< $tmpf")) {	# �W�J�p�Ɉꎞ�t�@�C���ɕۑ� �ǂݏ������[�h�ŊJ��
			} else {open(FILE, ">$tmpf");}

			binmode(FILE);
			if($win9x == 0){
				flock(FILE, 2);				# ���b�N�m�F�B���b�N
			}
			seek(FILE, 0, 0);			# �t�@�C���|�C���^��擪�ɃZ�b�g
			$len = length($buffer);
			syswrite(FILE, $buffer, $len);
			truncate(FILE, $len);	# �t�@�C���T�C�Y���������񂾃T�C�Y�ɂ���

			if($unzipmode == 3){
				close(FILE);				# close����Ύ����Ń��b�N����
			}

				# �W�J
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
				close(FILE);				# close����Ύ����Ń��b�N����
			}

			$| = 0;
		}
	}

	return $buffer;
	#return @data;
}

# �_�E�����Ă���z�X�g�������������A�_�E���������Ԃ�ݒ肷��B
sub founddown {
	$downinfo{"$_[0]"} = time();
}

# �z�X�g�̃_�E����Ԃ𒲂ׂ�B
# �_�E�������m���Ă���10���ȓ��͖������ŃA�N�Z�X���Ւf����B
sub checkdown {
	if(time() - $downinfo{"$_[0]"} < 60 * 10) {
		return -1;
	}
	return 0;
}

1;