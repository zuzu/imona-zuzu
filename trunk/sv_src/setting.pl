# iMona/dat.pl�ݒ�X�N���v�g

############# ���ʂ̐ݒ� ############
$win9x = 0;				# win9x�T�[�o�[�Ŏg�p����ꍇ��1�ɂ��Ă��������B
						# ���̏ꍇ�Aflock(�t�@�C���������ݎ��̃��b�N)���g�p�ł��܂���B
						# ����đ��l���Ŏg�p����ƕs����N����\���������ł��B

$windows = 0;			# windows�T�[�o�[(9x,2000,xp)�Ŏg�p����ꍇ��1�ɂ��Ă��������B
						# ����windows�T�[�o�[�Ŏg�p����ۂ̒��ӎ�������
						# $SIG{'ALRM'}���g�p�ł����A�T�[�o�[�ւ̐ڑ��̃^�C���A�E�g���ݒ�ł��Ȃ��̂ŁA
						# ���l���Ŏg�p���Ă���ƁA2ch�̃T�[�o�[�������Ă���Ƃ���
						# �A�N�Z�X�����܂��Ĉꏏ�ɗ����Ă��܂��\��������܂��B

$encodemod = 1;			# 0: jcode.pl���g�p����
						# 1: Encode���W���[�����g�p����(�����R�[�h�ϊ����x�����サ�܂��Bperl 5.8.x�ł͕W���Ŏg�p�ł��܂��B)
						# 2: Encode���W���[���ɉ����ADrk::Encode���W���[�����g�p����(�����R�[�h�ϊ�������ɍ����ɂȂ�܂��B)
						#    ������g�p���邽�߂ɂ́Ahttp://www.drk7.jp/MT/archives/000064.html (�������p�S�p���C�u����  Drk7jp)
						#    ���烂�W���[�����_�E�����[�h���ăC���X�g�[�����Ȃ���΂����܂���B

############# dat.pl���g�p����������̐ݒ� ############

#dat.pl
#$url = 'http://127.0.0.1/2c/';	#�ݒu����URL
$url = 'http://127.0.0.1:81/imona/';	#�ݒu����URL

############# iMona�p�T�[�o�[��ݒu����������̐ݒ� ############

#2.cgi
$rawmode = 0;			# 1:rawmode�̎g�p 0:DAT���ǂ�(2c.pl���K�v�ł�)
						# 2:rawmode�̎g�p+�ԍ�9xxx�̃A�N�Z�X������(2c.pl���K�v�ł�)

$writemode = 0;			# 0:$rawmode=0�̎���2.cgi�ŏ������݉�ʂ�html���o�͂����܂��B
						#   $rawmode=1�̎���r.i�փ��_�C���N�g���܂�(Location: �`���o��)
						# 1:�������ݎ��ɏ��r.i�փ��_�C���N�g���܂�(Location: �`���o��)
						# 2:�������ݎ��ɏ��2.cgi���������݉�ʂ�html���o�͂����܂��B
						#   (���̏ꍇ�A$rawmode=1���Ə������ރX���b�h�̃^�C�g�����\������܂���)
						# 2.cgi�ŏ������݉�ʂ��o�͂���ƃp�P�b�g�オ�ߖ�ł��A
						# r.i���g�p�ł��Ȃ��Ƃ��ł�������悤�ɂȂ�܂��B

$log = 1;				# 1:�A�N�Z�X���O��ۑ�����B 0:�ۑ����Ȃ��B

$packmode = 1;			# gzip,zip�]���g�p���̈��k���@
						# 0:gzip,zip�]���͂��Ȃ�
						# 1:zlib(Compress::Zlib)���g�p(����) ��Activeperl�ɕW���œ����Ă���悤�ł��B
						# 2:linux��gzip���g�p(zip�̏ꍇ�A�ꎞ�t�@�C���̍쐬���K�v�ł�)
						# 3:windows,(FreeBSD?)��gzip���g�p(�񐄏�)�B
						#   (zip�̏ꍇ�A�ꎞ�t�@�C���̍쐬���K�v�ł��B�܂��A���̕��@�̓t�@�C���̔r�����䂪�s���S�ł��B)
						
$tmpf2 = 'tmpfile2.gz';	# $packmode = 2;or$packmode = 3;�̎��Ɏg�p����e���|�����t�@�C���̃t�@�C����


$deldat = 0;			# 1:2.cgi��dat�̎����������s��(linux�I�p,windows�I�ɂ͖��Ή�)
						# cron���g�p�ł���ꍇ�́Acron�̎g�p�𐄏����܂��B
						# �R�}���h�̗�Ffind directory -type f -atime +2 -exec rm {} \; 
						# ���̏ꍇ�Adirectory�ȉ��ɂ���Q���ԃA�N�Z�X�̂Ȃ��t�@�C�����������܂��B

$deldatchk = 'deldatchk.txt';	# dat�̎��������Ɏg���t�@�C�����B
								# ���̃t�@�C���͉ߋ���dat�̎����������s����������ۑ����Ă��܂��B

$deldatst = 3;			# dat�̎����������s���J�n����
$deldatend = 6;			# dat�̎����������s���I������
						# $deldatst���`$deldatend���܂ł̊ԂɎ����������s���܂��B

$deldatdays = 2;		# ��������dat�t�@�C���̃A�N�Z�X�̂Ȃ������B

############# ���̑��̐ݒ�(�킩��Ȃ��Ƃ��͕ύX�̕K�v�͂���܂���) ############

#http.pl
$timeout = 5;			# �ڑ��̃^�C���A�E�g����(�P�ʁF�b)
$timeout2 = 30;			# �]���̃^�C���A�E�g����(�P�ʁF�b)

$usegzip = 1;			# 0:gzip�]�����g�p���Ȃ� 1:gzip�]�����g�p����
						# �����1�ɂ���ƁAAccept-Encoding: gzip�𑗐M���܂��B

$unzipmode = 1;			# gzip�]���g�p���̉𓀕��@
						# 1:zlib(Compress::Zlib)���g�p(����) ��Activeperl�ɕW���œ����Ă���悤�ł��B
						# 2:linux��gzip���g�p(�ꎞ�t�@�C���̍쐬���K�v�ł�)
						# 3:windows,(FreeBSD?)��gzip���g�p(�񐄏�)�B
						#   (�ꎞ�t�@�C���̍쐬���K�v�ł��B�܂��A���̕��@�̓t�@�C���̔r�����䂪�s���S�ł��B)

$tmpf = 'tmpfile.gz';	# $unzipmode = 2;or$unzipmode = 3;�̎��Ɏg�p����e���|�����t�@�C���̃t�@�C����


#2c.pl
$mode = 0;				# ���샂�[�h 0:�ʏ� 1:�O���Ăяo���̋֎~ 2:require�ł̌Ăяo���̋֎~

$readdiff = 1;			# �����ǂݍ��� 0:���Ȃ� 1:����

$brmininterval = 60;	# �X���ꗗ�̃L���b�V���̍ŏ��X�V�Ԋu(�b)�B�O��̃L���b�V���̍X�V��A
						# ���̎��Ԃ��o�߂��Ă��Ȃ���2ch����ǂݍ��݂��s���܂���B
$brmaxinterval = 86400;	# �X���ꗗ�̃L���b�V���̍ő�X�V�Ԋu(�b)�B�O��̃L���b�V���̍X�V��A
						# ����ȏ�̎��Ԃ��o�߂��Ă��X���ꗗ���X�V����Ă��Ȃ��ꍇ�A�ړ]���ꂽ�\��������̂ŃL���b�V�����g�p���܂���B
$thmininterval = 0;		# �X���b�h�̃L���b�V���̍ŏ��X�V�Ԋu(�b)�B�O��̃L���b�V���̍X�V��A
						# ���̎��Ԃ��o�߂��Ă��Ȃ���2ch����ǂݍ��݂��s���܂���B
$chkinterval = 10800;	# �X���b�h�̍X�V�`�F�b�N�Ԋu(�b)�B�O��̃L���b�V���̍X�V��A
						# ���̎��Ԃ��o�߂��Ă��Ȃ���2ch����ǂݍ��݂��s���Ă̍X�V�`�F�b�N�͍s���܂���B
						# �y�X���b�h�̍X�V�`�F�b�N���s�����߂̋@�\�ł������݂͍H�����ł��z

$dat = '../dat';			# DAT�i�[�ꏊ�BDAT�̊i�[���@�͈ȉ��̒ʂ�ł��B
						# \dat ->���炩���ߍ쐬���Ă����Ă��������B
						#   \AAA�@���̖���
						#     subject.txt
						#     123456789.dat
						#   \BBB
						#     subject.txt
						#     987654321.dat

$bruseims = 1;			# 1:�X���ꗗ���擾����Ƃ���Last-Modified���g�p�����X�V�`�F�b�N���g�p���� 0:�g�p���Ȃ�

$thuseims = 1;			# 1:�X���b�h���擾����Ƃ���Last-Modified���g�p�����X�V�`�F�b�N���g�p���� 0:�g�p���Ȃ�

$dirpermission = 0744;	# �V�����̖��̂̃f�B���N�g�����쐬����ꍇ�̃p�[�~�b�V����

$usenextthsearch = 0;	# ���X�����o�@�\���g�p����(perl����MeCab�����p�\�ł���K�v������܂�)

$zuzunextthsearch = 1;  # �ȈՎ��X�����o�@�\���g�p����

1;
