import "./Resume.css";

const Resume = () => {
  return (
    <section className="resume">
      <div className="resume-container">
        <h2 className="resume-title">Resume Management</h2>

        <p className="resume-subtitle">
          Upload your resume so you can use it when applying for jobs through CareerConnect.
        </p>

        <form>
          <div className="resume-upload-box">
            <label className="resume-upload-label">
              Upload your resume
            </label>

            <input
              type="file"
              accept=".pdf"
            />
          </div>

          <button className="resume-upload-button" type="button">
            Upload Resume
          </button>
        </form>
      </div>
    </section>
  );
};

export default Resume;