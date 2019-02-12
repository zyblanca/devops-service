#!/bin/bash
echo '------------------------------------------'
echo 'sh push_image.sh [harbor_url] [username] [password]\n'
echo '>>>     harbor_url 需要带上项目路径'
echo '------------------------------------------\n'
if [ x$1 != x ] ;then
    echo "harbor url: $1"
else
    echo "no harbor url!\nexit"
    exit 0
fi
if [ x$2 != x ] ;then
    echo "harbor user: $2"
else
    echo "no harbor user!\nexit"
    exit 0
fi

if [ x$3 != x ] ;then
    echo "harbor user password: $3"
else
    echo "no harbor user password!\nexit"
    exit 0
fi
docker login -u $2 -p $3 $1
lastch=`echo $1 | awk '{print substr($0,length,1)}'`
harobr_url=$1
if [  "$lastch" != "/" ] ;then
    harobr_url="$1/"
fi
cat images | while read line
do
    docker pull ${line}
    newline="$harobr_url${line##*/}"
    docker tag $line $newline
    docker push $newline
done