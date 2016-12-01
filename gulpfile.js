var gulp        = require('gulp');
//var sass        = require('gulp-sass');
var sass = require('gulp-ruby-sass');
var browserify = require('gulp-browserify');
var rename = require('gulp-rename');
var optimizeJs = require('optimize-js');
var gulpif = require('gulp-if');

var browserSync = require('browser-sync');
var sourcemaps = require('gulp-sourcemaps');

var cleanCSS = require('gulp-clean-css');
var minify = require('gulp-minify');
var autoprefixer = require('gulp-autoprefixer');
var run = require('gulp-run');



gulp.task('app', function() {
  var cmd = new run.Command('optimize-js ./_file/app-tmp.js > ./_file/app.js');
  gulp.src('_frontend/app/_main.msx')
      .pipe(browserify({
        transform: ['mithrilify']
      }))
      .pipe(rename('app-tmp.js'))
      .pipe(gulp.dest('_file/'))
      .on('end', function(){
        cmd.exec('');
      });
});

gulp.task('admin', function() {
  gulp.src('_frontend/admin/app.msx')
      .pipe(browserify({
        transform: ['mithrilify']
      }))
      .pipe(rename('admin.js'))
      .pipe(gulp.dest('_file/'))
});


gulp.task('sass', function () {
    return sass('_frontend/scss/*.scss')
        .on('error', sass.logError)
        .pipe(autoprefixer({
            browsers: ['last 2 versions'],
            cascade: false
        }))
        .pipe(rename('custom.css'))
        .pipe(gulp.dest('_file/'));
});


// gulp.task('ws', function(){
//   gulp.src('_frontend/ws/_main.js')
//       .pipe(browserify({
//         transform: ['mithrilify']
//       }))
//       .pipe(rename('ws2.js'))
//       .pipe(gulp.dest('public/javascripts/'))
// });

//
gulp.task('serve', function () {
  browserSync({
    // By default, Play is listening on port 9000
    proxy: 'localhost:9000',
    // We will set BrowserSync on the port 9001
    port: 9001,
    // Reload all assets
    // Important: you need to specify the path on your source code
    // not the path on the url
    files: [
      '_file/*.css',
      '_file/*.js',
      'public/stylesheets/*.css',
      'public/javascripts/*.js',
      'mainapp/views/{,*/}*.html',
      'mainapp/views/{,*/}*.stream',
      'mainapp/controllers/{,*/}*.scala',
      'conf/routes'],
    open: false
  });
});

gulp.task('watchjsx', ['app'], function () {
  gulp.watch('_frontend/app/{,*/}{,*/}*.msx', ['app']);
  gulp.watch('_frontend/admin/{,*/}{,*/}*.msx', ['admin']);
  gulp.watch('_frontend/scss/{,*/}*.scss', ['sass']);
});

// Creating the default gulp task
gulp.task('default', [  'app', 'admin', 'sass', 'watchjsx', 'serve']);
