const gulp = require('gulp');
const sass = require('gulp-sass');

gulp.task('sass', function(){
    //compile and copy our sass file
    gulp.src('dmz/src/main/resources/sass/**/*.scss')
        .pipe(sass())
        .pipe(gulp.dest('dmz/src/main/resources/static/css/'));
});

//watch task
gulp.task('default',function() {
    gulp.watch('dmz/src/main/sass/**/*.scss',['styles']);
});

//these get run by gradle when building the app
gulp.task('build', ['styles']);