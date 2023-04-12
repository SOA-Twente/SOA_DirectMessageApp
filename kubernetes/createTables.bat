kubectl config set-context --current --namespace=production
@REM kubectl get pods | findstr db-follow


@REM Creates table for db-follow
FOR /F "delims= " %%i IN ('kubectl get pods ^| findstr db-follow') DO set podname=%%i
@REM kubectl exec --stdin --tty %podname% -- /bin/bash -c "PGPASSWORD=root psql -h db-follow -U postgres db_follow -c 'SELECT * FROM followings;'"
kubectl exec --stdin --tty %podname% -- /bin/bash -c "PGPASSWORD=root psql -h db-follow -U postgres db_follow -c 'CREATE TABLE followings(id SERIAL PRIMARY KEY, user_id varchar NOT NULL,following_id varchar NOT NULL);'"

FOR /F "delims= " %%i IN ('kubectl get pods ^| findstr db-post-message') DO set podname=%%i
kubectl exec --stdin --tty %podname% -- /bin/bash -c "PGPASSWORD=root psql -h db-post-message -U postgres db_post_message -c 'CREATE TABLE quacks(id SERIAL PRIMARY KEY,user_id varchar NOT NULL,quack varchar(300) NOT NULL,is_reply boolean,reply_to_quack_id int,is_retweet boolean,retweet_of_quack_id int,created_at TIMESTAMP DEFAULT NOW());'"


FOR /F "delims= " %%i IN ('kubectl get pods ^| findstr db-register') DO set podname=%%i
kubectl exec --stdin --tty %podname% -- /bin/bash -c "PGPASSWORD=root psql -h db-register -U postgres db_register -c 'DROP TABLE users;'"
kubectl exec --stdin --tty %podname% -- /bin/bash -c "PGPASSWORD=root psql -h db-register -U postgres db_register -c 'CREATE TABLE users(id SERIAL PRIMARY KEY,username varchar(255) NOT NULL,email varchar(255) NOT NULL,password varchar(255));'"

FOR /F "delims= " %%i IN ('kubectl get pods ^| findstr db-profile') DO set podname=%%i
kubectl exec --stdin --tty %podname% -- /bin/bash -c "PGPASSWORD=root psql -h db-profile -U postgres db_profile -c 'CREATE TABLE userData(id int NOT NULL,username varchar(255) NOT NULL,description varchar(300),followers int,tags varchar(255),PRIMARY KEY (id));'"