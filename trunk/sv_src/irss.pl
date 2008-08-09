#!/usr/local/bin/perl

###################################################################################################

#
# irss.pl
#		// iMona RSS �z�M�X�N���v�g
#		// [charset:Shift-JIS]
#

###################################################################################################

#�y �T�v �z
#	���̃X�N���v�g�� 2ch �̃X���b�h�� RSS �Ŏ擾���邽�߂̃X�N���v�g�ł��B
#	XML::RSS ���W���[���� iMona �� dat �L���b�V���@�\�𗘗p���Ă��܂��B
#	���o�[�W�����ł̓G���[������S���s���Ă��܂��񂵁A���Ǘ]�n����������܂��B
#
#�y �Ăяo�����@ �z
#	http://�`/irss.pl/AAA.2ch.net/BOARDNAME/ => ���Ή�
#	http://�`/irss.pl/AAA.2ch.net/test/read.cgi/BOARDNAME/1234567890/
#	http://�`/irss.pl/AAA.2ch.net/test/read.cgi/BOARDNAME/1234567890/l50
#	http://�`/irss.pl/AAA.2ch.net/BOARDNAME/1234567890/123-456
#	�ȂǓK���ɁB
#
#�y �X�V���� �z
#	06/05/03 ver0.1
#	�E�����J

###################################################################################################

BEGIN {	# ����N�����̂�
	use XML::RSS;
	require '2c.pl';
	
	$maxres = 9999;	#�ő僌�X�ǂݍ��ݐ�����
}

print "Content-Type: text/xml\n\n";
&read();

exit();


sub read {
	$/ = "\x0A";	#���s�R�[�h��\x0A(LF)�ɂ���B
	binmode(STDOUT);

	$path = $ENV{PATH_INFO};

	# �T�[�o�̎擾
	$path =~ s/^\/?([^\/]*)\//\//;
	$server = $1;

	# ����Ȃ������폜
	$path =~ s|/i/|/|g;		$path =~ s|/read.cgi/|/|g;
	$path =~ s|/p.i/|/|g;	$path =~ s|/r.i/|/|g;
	$path =~ s|/test/|/|g;

	# �X���b�h�ԍ����擾
	if($path =~ /([0-9]{9,10})/) {	#���X�̕\��
		$ithread = $1;
		$path =~ s|/([0-9]{9,10})/|/|;
	}
	$path =~ s|//+|/|g;

	# �����擾
	if($path =~ /\/([A-Za-z0-9]{2,10})\// ){
		$board = $1;
	} else {	#�����擾�ł��Ȃ�
		#print "�G���[�ł�<br>�̖��O���擾�ł��܂���ł���<br>�������悤�Ƃ����f�[�^�F$_path<br>���݂̃f�[�^�F$path";
		return;
	}

	if($ithread != 0){	# ���X�̕\��
		if($path =~ /\/(l?)([0-9]+)(\-?)([0-9]*)$/){	# ���X�ʒu�w�肪����ꍇ
			if($1 ne ''){#last
				$last = $2;
				if($last > $maxres){$last = $maxres;}
				$op = 'f';
			} else {
				if($3 ne ''){#-
					$start = $2;
					if($4 eq '' || $4 - $2 > $maxres){
						$to = $start + $maxres;	$op = 'f';
					} else {
						$to = $4;
					}
				} else {	#1���X���w��
					$start = $2;
					$to = $2;
				}
			}
		} else {										# �����ꍇ
			$start = 1;	$to = $maxres; $op = 'f';
		}
		if($path =~ /\/[\w\-]*n[\w\-]*$/){	# no first
			$op =~ s/f//;
		}

		# 2ch �܂��̓L���b�V������f�[�^�̎擾
		$data = &p2chcache'read($server, $board, $ithread, $start, $to, $last, $op);
		@data = split(/\n/, $data);

		# �o��
		putresrss();
	} else {	# �̕\��
		#print $data;
	}
}

sub putresrss {
	
	# �w�b�_�����̉��
	my $title = (split(/<>/, $data[1]))[4];
	my ($st, $end, $all);
	$data[0] =~ m/Res:([0-9]*)\-([0-9]*)\/([0-9]*)/i;	#�n��-�I���/�S���̐�
	$st = $1;	$end = $2; $all = $3;
	if($end > $all){$end = $all;}

	# XML ���W���[���̏�����
	my $rss = new XML::RSS (version => '1.0', encoding => 'Shift_JIS');
	$rss->add_module(prefix => "content",
		uri => "http://purl.org/rss/1.0/modules/content/"
	);

	# �\������ RSS �̃��\�[�X URL �̌���
	$url = $ENV{PATH_INFO};
	$url =~ s|^.+/([^/]+?)$|$1|;
	$url = "http://" . $server . "/test/read.cgi/" . $board . "/" . $ithread . "/" . $url;

	$rss->channel(
		title        => $title,
		link         => $url,
		description  => $title,
	);
	
	if($st == 0){
		#print "�\\�����郌�X������܂���<br><br>(dat�����A���X�w��͈̓G���[�A�o�O�Ȃ�)";
		$end = $start - 1;
	} else {
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

			# �^�C�g��(���e�ҕ����̏���)
			$title = $resnum . " �F$res[0]";
			if($res[1] ne ''){ $title .= " [$res[1]]"; }	# ���[����
			$title .= " $res[2]";							# �����Ȃ�
			$title =~ s/<\/?b>//g;

			# �\������ RSS �̃��\�[�X URL �̌���
			$url = "http://" . $server . "/test/read.cgi/" . $board . "/" . $ithread . "/" . $resnum;

			$description = $res[3];
			$description =~ s/<br>/ /g;
			$description =~ s/<a [^>]+?>(.+?)<\/a>/$1/g;

			$content = $res[3];
			$content =~ s|<a href="../test/read.cgi|<a href="http://$server/test/read.cgi|g;
			
			$rss->add_item(
				title       => $title,
				link        => $url,
				description => $description,
				content => {
					'encoded' => "<![CDATA[$content]]>"
				},
			);

		}
	}
	print $rss->as_string;
}
