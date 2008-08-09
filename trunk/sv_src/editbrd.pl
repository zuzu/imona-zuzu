#!/usr/local/bin/perl

# xxx
#         soft.spdv.net


package peditbrd;

##/�ݒ蕔��/########################
$brd2 = 'brd2.txt';	# �f�[�^(board.cgi�ō쐬��������) �J�e�S����̖��O
$brd3 = 'brd3.txt';	# �f�[�^(board.cgi�ō쐬��������) �ԍ��|��URL�ϊ��p
$brd4 = 'brd4.txt';	# �V�����f�[�^(board.cgi�ō쐬��������) �J�e�S����̖��O
$brd5 = 'brd5.txt';	# �V�����f�[�^(board.cgi�ō쐬��������) �ԍ��|��URL�ϊ��p
$flexiblebrd = 'brdflex.txt';	# ver15�ȏ�Ŏg�p����ꗗ�f�[�^ �[���ɏo�͂���
####################################

sub makebrd {
	my $category = shift;
	my $boardname = shift;
	my $boardurl = shift;
	my $ret = 0;

	for(1..2){
		$categoryindex = -1;
		if(&openbrd($_ * 2) == 1){	# brd2 brd4
			$i = 0;
			foreach $line (@line) {
				$categorytmp = $line;
				$categorytmp =~ s/[\n\r]*$//;
				if($category eq $categorytmp){
					$categoryindex = $i;
				}
				$i++;
				if($line eq "\n"){last;}
			}

			if($categoryindex != -1){
				$line[$i + $categoryindex] =~ s/[\n\r]*$//;
				$line[$i + $categoryindex] .= "\t" . $boardname;
				&savebrd($_ * 2);
			}
		}

		if($categoryindex != -1 && &openbrd($_ * 2 + 1) == 1){	# brd3 brd5
			$line[$categoryindex] =~ s/[\n\r]*$//;
			$line[$categoryindex] .= "\t" . $boardurl;

			&savebrd($_ * 2 + 1);
			$ret = 1;
		}
	}
	return $ret;
}

sub transfer {
	my $str = $_[0];
	
	&openbrd(3);
	$ret = &changeborad($str);
	if($ret == 1){
		&savebrd(3);
	}

	&openbrd(5);
	$ret2 = &changeborad($str);
	if($ret2 == 1){
		&savebrd(5);
	}
	return $ret2;
}

sub openbrd{
	if(open(DATA, ${'brd' . $_[0]})){
		binmode(DATA);
		@line = <DATA>;
		close(DATA);
		return 1;
	}
	return 0;
}

sub changeborad {
	$url = $_[0];
	if($url =~ m|http://([^\.]*)\.([^/]*)/([^/]*)/|){
		$server = $1;
		$domain = $2;
		$board = $3;
	} else {
		return -3;
	}

	$hit = 0;
	if($#line > 0){
		for($i = 0; $i <= $#line; $i++){
			$line[$i] =~ s/[\r\n]*$//;
			
			if($line[$i] =~ m|http://([^\.]*)\.$domain/$board/|){
				$hit = 1;
				$old = $1;
				if($old eq $server){
					$changed = 1;
				} else {
					$line[$i] =~ s|http://[^\.]*\.$domain/$board/|http://$server\.$domain/$board/|g;
					$changed = 1;
				}
			}
		}

		if($hit == 0){	#�q�b�g���Ȃ�����
			return -1;
		}
		if($changed == 0){	#���łɕύX����Ă���
			return 0;
		}
		return 1;
	} else {	#�t�@�C���̃I�[�v���Ɏ��s����
		return -2;
	}
}


sub savebrd {
	#�o�͌`��(2)�ɕϊ�
	$buf = '';
	for($i = 0; $i <= $#line; $i++){
		if($line[$i] eq ''){
			next;
		}
		$line[$i] =~ s/[\r\n]*$//;
		$buf .= $line[$i] . "\n";
	}

	$buf =~ s/[\r\n]*$//;

	if(open(DATA, "+< ${'brd' . $_[0]}")){
		binmode(DATA);
		flock(DATA, 2);				# ���b�N�m�F�B���b�N

		seek(DATA, 0, 0);			# �t�@�C���|�C���^��擪�ɃZ�b�g

		print DATA $buf;

		truncate(DATA, tell(DATA));	# �t�@�C���T�C�Y���������񂾃T�C�Y�ɂ���
		close(DATA);
	}
}

sub cachebrd5{
	# �ꗗ���L���b�V������
	if($#brd5cache < 0 || (stat("$brd5"))[9] != $brd5mtime){
		@brd5cache = ();
		if(open(DATA, "$brd5")){
			binmode(DATA);
			@brd5cache = <DATA>;
			close(DATA);
			for($i = 0; $i <= $#brd5cache; $i++){
				$brd5cache[$i] =~ s/[\r\n]*$//;
				@{$brd5splitcache[$i]} = split(/\t/, $brd5cache[$i]);
			}
		}
		$brd5mtime = (stat("$brd5"))[9];
	}
}


# �̏�������T�[�o���擾
sub sbrd2server {
	my $brd;
	$brd = $_[0];

	$brd =~ s/[^\w]//g;	# sanitize
	
	if($#brd5cache < 0){
		cachebrd5();
	}

	foreach (@brd5cache) {
		if (m%http://([^/]+?\.(net|com))/$brd/%) {
			return $1;
		}
	}
	return "";
}

sub url2nbrd{	#URL��ԍ��ɕϊ�
	my ($i, @line, @line2);
	
	if($#brd5cache < 0){
		if(open(DATA, "$brd5")){
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
		if($line[$i] =~ /^(.*?)$_[0]/){
			@line2 = split(/\t/, $1);
			if($#line2 < 0){
				return $i * 100;
			} else {
				#return $i * 100 + ($#line2 - 1);
				return $i * 100 + $#line2;
			}
		}
	}
	return -1;
}

sub getboardname {	#�̔ԍ���������擾
	if(open(DATA, "$brd4")){
		binmode(DATA);
		while(<DATA>){if($_ =~ /^[\r\n]+$/){last;}}	#���s�݂̂̍s�܂ł͂���Ȃ�
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


sub boradurl {	#�̔ԍ�����URL���擾
	$i = 0;
	@line = ();

	if($_[0] >= 1000000 && $_[0] < 2500000){
		my @category = ('auto','computer','game','movie','music','shop','sports','travel','business','study','news','otaku','anime','comic','school');

		return "http://$hostshitaraba/" . $category[int($_[0] / 100000 - 10)] . "/" . $_[0] % 100000 . "/";
	} elsif($_[0] >= 9000 && $_[0] < 10000){
		return "http://local/$_[0]/";
	}

	$i = int($_[0] / 100);

	if($#brd5cache < 0){
		if(open(DATA, "$brd5")){
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
	} else {	#�ꗗ���L���b�V������Ă���ꍇ
		@line = @brd5cache;

		return $brd5splitcache[$i][$_[0] - $i * 100];
	}
	return '';
}












return 1;
