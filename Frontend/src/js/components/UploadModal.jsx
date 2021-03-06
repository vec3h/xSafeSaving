import React from "react";

const UploadModal = React.createClass({
    getInitialState: function() {
        return {url: ''};
    },
    handleUrlChange: function(e) {
        this.setState({url: e.target.value});
    },
    checkProps: function () {
        let that = this;
        let $form = $('.ui.form');
        const validationRules = {
            fields: {
                file: {
                    identifier: 'media',
                },
                url: {
                    identifier: 'url',

                },
            },
            onSuccess: function (evt, fields) {
                evt.preventDefault();
                if (fields.media == '' && fields.url == '') {
                    that.addError('Enter valid file, or URL')
                } else {
                    if (fields.media != '') {
                        const data = new FormData();
                        $.each($('#media')[0].files, function (i, file) {
                            data.append('file-' + i, file);
                        });
                        that.props.uploadFile('UPLOAD_FILES', data);
                        document.getElementById('media').value = '';
                    } else {
                        that.props.uploadFile('UPLOAD_FROM_LINK', {url: fields.url});
                        that.setState({url: ''});
                    }
                }

            }
        };
        $form.form(validationRules)
    },
    addError: function (errText) {
        let that = this;
        let $formError = $('.ui.error.message');
        let $form = $('#'+that.props.modalId).find('.ui.form');
        $form.addClass('error');
        $formError.html(errText);
    },
    render: function () {
        let that = this;
        let $modal = $('#' + that.props.modalId);
        let $form = $modal.find('.ui.form');
        this.checkProps();
        this.props.error ? this.addError(this.props.error): $form.removeClass('error');
        this.props.fetching ? $form.addClass('loading'): ($form.removeClass('loading'));

        return (
            <div className='ui small modal' id={this.props.modalId}>
                <div className='header'>{this.props.headerName}</div>
                <div className='content'>
                    <form className='ui form'>
                        <div className='field'>
                            <div className='ui left icon input'>
                                <input
                                    id='media'
                                    type='file'
                                    name='media'
                                    multiple/>
                                <i className='file icon'/>
                            </div>
                        </div>
                        <div className='ui horizontal divider'>
                            Or
                        </div>
                        <div className='field'>
                            <div className='ui labeled input'>
                                <div className='ui label'>
                                    http://
                                </div>
                                <input type='text'
                                       placeholder='Enter URL'
                                       name='url'
                                       id='urldownload'
                                       value={this.state.url}
                                       onChange={this.handleUrlChange}/>
                            </div>
                        </div>
                        <div className='ui error message'></div>
                        <div className='actions'>
                            <div className='ui primary submit button'>Upload</div>
                            <div className='ui cancel button'>Cancel</div>
                        </div>
                    </form>
                </div>
            </div>
        );
    }
});
export default UploadModal